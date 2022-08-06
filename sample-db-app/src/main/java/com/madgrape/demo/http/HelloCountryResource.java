package com.madgrape.demo.http;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.madgrape.demo.config.CrossPlaneSecretBinding;
import com.madgrape.demo.db.CosmosConnectionProducer;
import com.madgrape.demo.model.HelloCountry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static io.quarkus.vertx.web.Route.HttpMethod.GET;
import static io.quarkus.vertx.web.Route.HttpMethod.POST;

@ApplicationScoped
@RouteBase(path = "/hello")
public class HelloCountryResource {

    @ConfigProperty(name = "HelloCountry.datasource-ref")
    protected String datasource;

    @ConfigProperty(name = "HelloCountry.container-name")
    protected String containerName;

    @ConfigProperty(name = "HelloCountry.db-name")
    protected String dbname;

    @Inject
    CrossPlaneSecretBinding ServiceBindingValues;

    @Inject
    protected CosmosConnectionProducer connection;

    @Inject
    MeterRegistry registry;
    private CosmosDatabase database;
    private CosmosContainer container;

    @PostConstruct
    protected void configureDB() {
        database = connection.getClients().getClientByDatasourceName(datasource).getDatabase(dbname);
        container = database.getContainer(containerName);
    }

    @Route(methods = GET, type = Route.HandlerType.BLOCKING, path = ":country", produces = "text/plain")
    public String hello(RoutingContext rc) {
        HelloCountry hc = new HelloCountry();
        String country = rc.pathParam("country");
        hc.setHello(country);
        hc.setId(String.valueOf(System.currentTimeMillis()));

        CosmosItemResponse item = container.createItem(hc, new PartitionKey(hc.getId()), new CosmosItemRequestOptions());
        registry.counter("country_counter", Tags.of("name", country)).increment();
        return "Hello " + hc.getHello() + "!";
    }

    @Route(methods = POST, type = Route.HandlerType.BLOCKING, path = "/create", produces = "text/plain", consumes = "application/json")
    public String helloPost(@Body HelloCountry hc) {
        CosmosItemResponse item = container.createItem(hc, new PartitionKey(hc.getId()), new CosmosItemRequestOptions());

        registry.counter("country_counter", Tags.of("name", hc.getHello())).increment();
        return "Hello " + hc.getHello() + "!";
    }
}