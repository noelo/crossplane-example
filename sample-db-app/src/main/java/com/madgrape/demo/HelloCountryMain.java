package com.madgrape.demo;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class HelloCountryMain {
    public static void main(String ... args) {
        Quarkus.run(args);
    }
}
