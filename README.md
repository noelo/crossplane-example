# Azure CosmosDB, CrossPlane, Quarkus & ServiceBinding Operator

This repository contains a simple application to demonstrate

1. Automated creation of Azure native resources using CrossPlane
2. Binding of these resources into a simple application via Service Binding Operator

## CrossPlane setup and configuration

### Prerequisites

```bash
export SUBSC_ID=xxxxxxx

az login
az account set -s ${SUBSC_ID}
az account show
az account list
```

### Create service principal and grant permissions

```bash
az ad sp create-for-rbac --sdk-auth --role Owner --scopes /subscriptions/${SUBSC_ID} > "creds.json"

export AZURE_CLIENT_ID=$(jq -r ".clientId" < "./creds.json")
export RW_ALL_APPS=1cda74f2-2616-4834-b122-5cb1b07f8a59
export RW_DIR_DATA=78c8a3c8-a07e-4b9e-af1b-b5ccab50a175
export AAD_GRAPH_API=00000002-0000-0000-c000-000000000000

az ad app permission add --id "${AZURE_CLIENT_ID}" --api ${AAD_GRAPH_API} --api-permissions ${RW_ALL_APPS}=Role ${RW_DIR_DATA}=Role
az ad app permission grant --id "${AZURE_CLIENT_ID}" --api ${AAD_GRAPH_API} --scope /subscriptions/${SUBSC_ID}
az ad app permission admin-consent --id "${AZURE_CLIENT_ID}"
```

### Setup Kind, install CrossPlane and Azure Provider

```bash
export KIND_EXPERIMENTAL_PROVIDER=podman kind create cluster

kubectl create namespace crossplane-system

helm repo add crossplane-stable https://charts.crossplane.io/stable
helm repo update
helm install crossplane --namespace crossplane-system crossplane-stable/crossplane

kubectl config set-context --current --namespace=crossplane-system
kubectl crossplane install provider crossplane/provider-jet-azure:v0.9.0
```

### Build and install Azure provided

First create secret with credentials

```bash
kubectl create secret generic azure-creds -n crossplane-system --from-file=key=./creds.json
```

Then create a ProviderConfig referencing the secret

```yaml
apiVersion: azure.jet.crossplane.io/v1alpha1
kind: ProviderConfig
metadata:
  name: default
spec:
  credentials:
    source: Secret
    secretRef:
      namespace: crossplane-system
      name: azure-creds
      key: key
```

## Installation via CrossPlane Resource Composition

The installation of the CRD and Composite definition only need to be done once.

A _claim_ is used to drive the individual creation of the set of resources.

### **ResourceGroup->Account->Database->Container**

#### Install the CRD

```bash
kubectl apply -f compose/nosqldb-cosmosdb-xrd.yml
kubectl get CompositeResourceDefinition
```

#### Install the Composite definition

```bash
kubectl apply -f compose/nosqldb-cosmosdb-composite.yaml
kubectl describe composition.apiextensions.crossplane.io/azure-cosmosdb-composition
```

#### Create a claim for the set of resource

```bash
kubectl apply -f compose/nosqldb-cosmosdb-claim.yaml
kubectl describe nosqldb.runtime.madgrape.com/noctestdb
```

**NOTE** The connection settings will be store in a secret called _**nosqltestdb-connection-details**_ .
This is the secret referenced in the service-binding resource.

## Service Binding Operator

### Install Operator Lifecycle Manager

```bash
curl -sL https://github.com/operator-framework/operator-lifecycle-manager/releases/download/v0.21.2/install.sh | bash -s v0.21.2
```

### Install the Service Binding Operator

```bash
kubectl create -f https://operatorhub.io/install/service-binding-operator.yaml
```

### Create a sample application

```bash
cd  sample-db-app

## compile build and push the image to your registry
quarkus build -Dquarkus.container-image.build=true -Dquarkus.container-image.push=true

##Deploy the kubernetes resources
cd target/kubernetes

kubectl apply -f kubernetes.yml
```

### Pod crashes with Crashloop backoff

```txt
Failed to load config value of type class java.lang.String for: quarkus.service-binding.nosqldb-binding.primary_readonly_connectionFailed to load config value of type class java.lang.String for: quarkus.service-binding.nosqldb-binding.primary_connectionFailed to load config value of type class java.lang.String for: quarkus.service-binding.nosqldb-binding.secondary_connectionFailed to load config value of type class java.lang.String for: quarkus.service-binding.nosqldb-binding.secondary_readonly_connection
```

The reason for this is that we have to bind the _CrossPlane_ created secrets into the _Pod_.

### Now create and deploy the Service Binding resource

```yaml
apiVersion: servicebinding.io/v1alpha3
kind: ServiceBinding
metadata:
  name: nosqldb-binding
spec:
  type: runtime.madgrape.com/connection
  service:
    apiVersion: v1
    kind: Secret
    name: nosqltestdb-connection-details
  workload:
    apiVersion: apps/v1
    kind: Deployment
    name: helloworldcosmos
```

```bash
kubectl create -f crossplane-example/service-binding/sample-app-service-binding.yml
```

### Test

```bash
curl http://<route>/hello/testtest
```

Use Data Explorer in the Azure console to validate values are being inserted into the database.

### Under the covers

The service binding operator binds the secret into the pod location defined by the environment variable SERVICE_BINDING_ROOT

```bash
oc describe pod helloworldcosmos-556cfc9448-dvpm7|grep -i binding
      SERVICE_BINDING_ROOT:  /bindings
      /bindings/nosqldb-binding from nosqldb-binding (rw)
  nosqldb-binding:
    SecretName:  nosqldb-binding-b4d29a16
```

The Quarkus Runtime loads any secrets found in that location as long as service binding is enabled in the applications.properties

```txt
quarkus.kubernetes-service-binding.enabled=true
```

The contents of these secrets are then made available to the Quarkus configuration system and can be accessed in the code.

See [sample-db-app/src/main/java/com/madgrape/demo/config/CrossPlaneSecretBinding.java](sample-db-app/src/main/java/com/madgrape/demo/config/CrossPlaneSecretBinding.java)

```java
public class CrossPlaneSecretBinding {

    @ConfigProperty(name = "quarkus.service-binding.nosqldb-binding.primary_connection")
    String primaryConn;
```
