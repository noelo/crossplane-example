# Installation using individual CrossPlane resource definitions

## **ResourceGroup->Account->Database->Container**

### Set up the Resource Group

```yaml
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

See [base-account.yml](base/base-account.yml)

```yaml
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

See [base-sqldb.yml](base/base-sqldb.yml)

```yaml
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

See [base-sql-container.yml](base/base-sql-container.yml)

```yaml
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

## Nuke from Orbit

```bash
kubectl delete NoSQLDb noctestdb
kubectl delete Composition azure-cosmosdb-composition
kubectl delete CompositeResourceDefinition znosqldbs.runtime.madgrape.com
```
