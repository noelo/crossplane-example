package com.madgrape.demo.config;

import io.smallrye.config.ConfigMapping;

import java.util.Set;

@ConfigMapping(prefix = "cosmosdbclient")
public interface CosmosDBClientConfig {
    Set<DataSource> datasource();

    interface DataSource {
        String datasourceName();

        Config config();

        interface Config {
            String azureRegionName();
        }
    }


}