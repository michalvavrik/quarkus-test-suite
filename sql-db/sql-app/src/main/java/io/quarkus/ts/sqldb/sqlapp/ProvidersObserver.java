package io.quarkus.ts.sqldb.sqlapp;

import java.security.Provider;
import java.security.Security;

import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.StartupEvent;

public class ProvidersObserver {

    void observe(@Observes StartupEvent event) {
        for (Provider provider : Security.getProviders()) {
            System.out.println("registered provider is " + provider.getName());
        }
    }

}
