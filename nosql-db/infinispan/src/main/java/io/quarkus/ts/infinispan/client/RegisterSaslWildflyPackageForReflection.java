package io.quarkus.ts.infinispan.client;

import java.security.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import jakarta.enterprise.event.Observes;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.util.SaslUtils;
import org.infinispan.commons.util.Util;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.security.sasl.SaslClientFactory;

@RegisterForReflection(classNames = {
        "com.sun.security.sasl.ClientFactoryImpl",
        "com.sun.security.sasl.ntlm.FactoryImpl",
        "com.sun.security.sasl.digest.FactoryImpl"
})
public class RegisterSaslWildflyPackageForReflection {

    private static final Provider[] SECURITY_PROVIDERS;

    void observe(@Observes StartupEvent event) {
        Collection<SaslClientFactory> clientFactories = SaslUtils
                .getSaslClientFactories(this.getClass().getClassLoader(), SECURITY_PROVIDERS, true);
        for (SaslClientFactory clientFactory : clientFactories) {
            System.out.println("client factory is " + clientFactory.getClass().getName());
        }
    }

    static {
        List<Provider> providers = new ArrayList();
        Iterator var1 = Arrays.asList("org.wildfly.security.sasl.plain.WildFlyElytronSaslPlainProvider",
                "org.wildfly.security.sasl.digest.WildFlyElytronSaslDigestProvider",
                "org.wildfly.security.sasl.external.WildFlyElytronSaslExternalProvider",
                "org.wildfly.security.sasl.oauth2.WildFlyElytronSaslOAuth2Provider",
                "org.wildfly.security.sasl.scram.WildFlyElytronSaslScramProvider",
                "org.wildfly.security.sasl.gssapi.WildFlyElytronSaslGssapiProvider",
                "org.wildfly.security.sasl.gs2.WildFlyElytronSaslGs2Provider").iterator();

        while (var1.hasNext()) {
            String name = (String) var1.next();
            Provider provider = (Provider) Util.getInstance(name, RemoteCacheManager.class.getClassLoader());
            providers.add(provider);
        }

        SECURITY_PROVIDERS = (Provider[]) providers.toArray(new Provider[0]);
    }
}
