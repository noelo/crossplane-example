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

# Installation via Crossplane individual resources 

## **ResourceGroup->Account->Database->Container**


## Set up the Resource Group
### Resourcegroup.yaml
```
apiVersion: azure.jet.crossplane.io/v1alpha2
kind: ResourceGroup
metadata:
  name: noctestjetrg
spec:
  forProvider:
    location: westus
  providerConfigRef: 
    name: default
```


## Set up the CosmosDB Account
### base-account.yaml
```
apiVersion: cosmosdb.azure.jet.crossplane.io/v1alpha2
kind: Account
metadata:
  name: azure-cosmosdb-account
  labels:
    provider: default
spec:
  forProvider: 
    kind: GlobalDocumentDB 
    location: westus 
    offerType: Standard 
    resourceGroupName: noctestjetrg
    backup: 
    - type: Continuous 
    consistencyPolicy:
    - consistencyLevel: Session 
    geoLocation:
    - failoverPriority: 0 
      location: "westus" 
      zoneRedundant: False 
  providerConfigRef:
    name: default
  writeConnectionSecretToRef:
    name: cosmosdb-secret
    namespace: crossplane-system
```

* Connection Strings will be store in the secret named **cosmosdb-secret** 

## Set up the Database
### base-sqldb.yaml
```
apiVersion: cosmosdb.azure.jet.crossplane.io/v1alpha2
kind: SQLDatabase
spec:
  forProvider:
    resourceGroupName: noctestjetrg
    accountName: azure-cosmosdb-account
    throughput: 400
  providerConfigRef:
    name: default
metadata:
  name: azure-cosmosdb-sqldb
  labels:
    provider: default
```

## Set up the Container
### base-sql-container.yaml
```
apiVersion: cosmosdb.azure.jet.crossplane.io/v1alpha2
kind: SQLContainer
spec:
  forProvider:
    resourceGroupName: noctestjetrg
    accountName: azure-cosmosdb-account
    databaseName: azure-cosmosdb-sqldb
    indexingPolicy:
    - excludedPath:
      - path: /excluded/?
      includedPath:
      - path: /*
      - path: /included/?
      indexingMode: Consistent
    partitionKeyPath: /definition/id
    partitionKeyVersion: 1
    throughput: 400
    uniqueKey:
    - paths:
      - /definition/idlong
      - /definition/idshort
  providerConfigRef:
    name: default
metadata:
  name: azure-cosmosdb-sqldb-container
  labels:
    provider: default
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

### Create a claim for a resource
```
oc apply -f noc-cosmosdb-claim.yaml
oc describe nosqldb.runtime.madgrape.com/noctestdb
```

### Nuke from Orbit
```
oc delete NoSQLDb noctestdb
oc delete Composition azure-cosmosdb-composition
oc delete CompositeResourceDefinition znosqldbs.runtime.madgrape.com
```
