package com.madgrape.demo.db;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.madgrape.demo.config.CosmosDBClientConfig;
import com.madgrape.demo.config.CrossPlaneSecretBinding;
import io.quarkus.logging.Log;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;

@Singleton
public class CosmosConnectionProducer {
    @Inject
    CrossPlaneSecretBinding ServiceBindingValues;

    @Inject
    CosmosDBClientConfig dbs;

    private static DBClients dbClients;

    private void initClient() {
        Log.info("Creating Cosmos Clients");
        dbClients = new DBClients(dbs.datasource().size());

        dbs.datasource().forEach(db -> {
            ArrayList<String> preferredRegions = new ArrayList<String>();
            preferredRegions.add(db.config().azureRegionName());

            //  Create sync client

            String cosmosEndpoint = ServiceBindingValues.getPrimaryConnectionDetails().get("AccountEndpoint");
            CosmosClient client = new CosmosClientBuilder()
                    .endpoint(ServiceBindingValues.getPrimaryConnectionDetails().get("AccountEndpoint"))
                    .key(ServiceBindingValues.getPrimaryConnectionDetails().get("AccountKey"))
                    .preferredRegions(preferredRegions)
                    .userAgentSuffix("HelloCosmos")
                    //TODO : We need to verify if this level is correct
                    .consistencyLevel(ConsistencyLevel.EVENTUAL)
                    .directMode()
                    .buildClient();

            dbClients.addClient(db.datasourceName(),client);
        });
    }

    @Produces
    public DBClients getClients()
    {
        if (dbClients == null) initClient();
        return dbClients;
    }
}