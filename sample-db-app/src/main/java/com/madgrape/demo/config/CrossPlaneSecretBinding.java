package com.madgrape.demo.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class CrossPlaneSecretBinding {

    @ConfigProperty(name = "quarkus.service-binding.nosqldb-binding.primary_connection")
    String primaryConn;

    public Map<String,String> getPrimaryConnectionDetails(){
        return parseConnectionString(primaryConn);
    }

    @ConfigProperty(name = "quarkus.service-binding.nosqldb-binding.secondary_connection")
    String secondaryConn;

    public Map<String,String> getsecondaryConnectionDetails(){
        return parseConnectionString(secondaryConn);
    }

    @ConfigProperty(name = "quarkus.service-binding.nosqldb-binding.primary_readonly_connection")
    String primaryConn_ReadOnly;

    public Map<String,String> getPrimaryConnectionDetails_ReadOnly(){
        return parseConnectionString(primaryConn_ReadOnly);
    }

    @ConfigProperty(name = "quarkus.service-binding.nosqldb-binding.secondary_readonly_connection")
    String secondaryConn_ReadOnly;

    public Map<String,String> getSecondaryConnectionDetails_ReadOnly(){
        return parseConnectionString(secondaryConn_ReadOnly);
    }

    public static Map<String,String> parseConnectionString(final String value) {
        HashMap<String,String> res = new HashMap();
        String[] sProperties = value.split(";");

        for (String prop:sProperties) {
            String[] tokens = prop.split("=",2);
            res.put(tokens[0],tokens[1]);
        }
        return res;
    }
}
