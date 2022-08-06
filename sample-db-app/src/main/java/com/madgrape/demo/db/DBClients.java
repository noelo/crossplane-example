package com.madgrape.demo.db;

import com.azure.cosmos.CosmosClient;
import io.quarkus.logging.Log;

import java.util.HashMap;

public class DBClients {

    private HashMap<String, CosmosClient> clientMap;

    public DBClients(int size) {
        clientMap = new HashMap<>(size);
    }

    public void addClient(String name, CosmosClient client) {
        if (clientMap.containsKey(name)) {
            Log.warn("duplicate key found in client map, replacing existing value-> " + name);
            clientMap.replace(name, client);
        } else
            clientMap.put(name, client);
    }

    public CosmosClient getClientByDatasourceName(String dsName) {
        if (!clientMap.containsKey(dsName))
            throw new RuntimeException("DataSource not found "+dsName);

        return clientMap.get(dsName);
    }
}
