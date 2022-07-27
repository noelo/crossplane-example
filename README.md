# Azure CosmosDB, CrossPlane, Quarkus & ServiceBinding Operator

## A simple application to demonstrate
1. Automated creation of Azure native resources using Crossplane 
1. Binding of these resources into a simple application via Service Binding Operator


# CrossPlane setup and configuration
## Prereqs
1. SUBSC_ID=xxxxxxx
1. ```az account set -s ${SUBSC_ID}```
1. az account show
1. az account list

## Create service principal and grant permissions
1. ```az ad sp create-for-rbac --sdk-auth --role Owner --scopes /subscriptions/${SUBSC_ID} > "creds.json"```
1. ```export AZURE_CLIENT_ID==$(jq -r ".clientId" < "./creds.json")```
1. RW_ALL_APPS=1cda74f2-2616-4834-b122-5cb1b07f8a59
1. RW_DIR_DATA=78c8a3c8-a07e-4b9e-af1b-b5ccab50a175
1. AAD_GRAPH_API=00000002-0000-0000-c000-000000000000
1. ``` az ad app permission add --id "${AZURE_CLIENT_ID}" --api ${AAD_GRAPH_API} --api-permissions ${RW_ALL_APPS}=Role ${RW_DIR_DATA}=Role```
1. ```az ad app permission grant --id "${AZURE_CLIENT_ID}" --api ${AAD_GRAPH_API} --scope /subscriptions/${SUBSC_ID}``` 
1. ```az ad app permission admin-consent --id "${AZURE_CLIENT_ID}" ```


## Setup Kind, install Crossplane and Azure Provider 
1. KIND_EXPERIMENTAL_PROVIDER=podman kind create cluster
1. kubectl create namespace crossplane-system
1. helm repo add crossplane-stable https://charts.crossplane.io/stable
1. helm repo update
1. helm install crossplane --namespace crossplane-system crossplane-stable/crossplane
1. kubectl config set-context --current --namespace=crossplane-system
1. kubectl crossplane install provider crossplane/provider-jet-azure:v0.9.0

## Build and install Azure provided

### Create secret with credentials
1. kubectl create secret generic azure-creds -n crossplane-system --from-file=key=./creds.json

### Provider.yaml 
```
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

# Installation via Crossplane Resource Composition

## **ResourceGroup->Account->Database->Container**

### Install the CRD
```
oc apply -f noc-cosmosdb-xrd.yml
oc get CompositeResourceDefinition
```

### Install the Composite definition
```
oc apply -f noc-cosmosdb-composite.yaml 
oc describe composition.apiextensions.crossplane.io/azure-cosmosdb-composition
```

### Create a claim for the set of resource
```
oc apply -f noc-cosmosdb-claim.yaml
oc describe nosqldb.runtime.madgrape.com/noctestdb
```

**NOTE** The connection settings will be store in a secret called _**noctestdb-cosmosdb-conn**_ . 
This is the secret referenced in the service-binding resource.

# Service Binding Operator

## Install Operator Lifecycle Manager
```
curl -sL https://github.com/operator-framework/operator-lifecycle-manager/releases/download/v0.21.2/install.sh | bash -s v0.21.2
```

## Install the Service Binding Operator
```
kubectl create -f https://operatorhub.io/install/service-binding-operator.yaml
```

## Create a sample application
```
kubectl create deployment hello-node --image=k8s.gcr.io/echoserver:1.4
```

## Create and apply a service binding
```
apiVersion: servicebinding.io/v1alpha3
kind: ServiceBinding
metadata:
  name: nosqldb-binding
spec:
  type: runtime.madgrape.com/connection
  service:
    apiVersion: v1
    kind: Secret
    name: noctestdb-cosmosdb-conn
  workload:
    apiVersion: apps/v1
    kind: Deployment
    name: hello-node
```


