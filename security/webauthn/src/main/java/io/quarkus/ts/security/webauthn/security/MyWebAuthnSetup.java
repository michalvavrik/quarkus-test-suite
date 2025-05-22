package io.quarkus.ts.security.webauthn.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnUserProvider;
import io.quarkus.ts.security.webauthn.model.User;
import io.quarkus.ts.security.webauthn.model.WebAuthnCredential;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class MyWebAuthnSetup implements WebAuthnUserProvider {

    @WithTransaction
    @Override
    public Uni<List<WebAuthnCredentialRecord>> findByUsername(String username) {
        System.out.println("//// find by username: " + username);
        return WebAuthnCredential.findByUsername(username)
                .invoke(list -> System.out.println("////////// find by username list: " + list))
                .onFailure().invoke(t -> System.out.println("msg iiiiiisssssssssss " + t.getMessage() + " and " + t))
                .map(list -> list.stream()
                        .map(WebAuthnCredential::toWebAuthnCredentialRecord).toList());
    }

    @WithTransaction
    @Override
    public Uni<WebAuthnCredentialRecord> findByCredentialId(String credentialId) {
        System.out.println("////////// findByCredentialId " + credentialId);
        return WebAuthnCredential.findByCredentialId(credentialId)
                .invoke(list -> System.out.println("////////// find by username list: " + list))
                .onFailure().invoke(t -> System.out.println("msg iiiiiisssssssssss " + t.getMessage() + " and " + t))
                .onItem().ifNull().failWith(() -> new RuntimeException("No such credentials"))
                .map(WebAuthnCredential::toWebAuthnCredentialRecord);
    }

    @WithTransaction
    @Override
    public Uni<Void> store(WebAuthnCredentialRecord credentialRecord) {
        System.out.println("////////// store " + credentialRecord);
        User newUser = new User();
        newUser.username = credentialRecord.getUsername();
        WebAuthnCredential credential = new WebAuthnCredential(credentialRecord, newUser);
        return credential.persist()
                .invoke(list -> System.out.println("////////// find by username list: " + list))
                .onFailure().invoke(t -> System.out.println("msg iiiiiisssssssssss " + t.getMessage() + " and " + t))
                .flatMap(c -> newUser.persist())
                .onItem().ignore().andContinueWithNull();
    }

    @WithTransaction
    @Override
    public Uni<Void> update(String credentialId, long counter) {
        System.out.println("////////// update " + credentialId);
        return WebAuthnCredential.findByCredentialId(credentialId)
                .invoke(list -> System.out.println("////////// find by username list: " + list))
                .onFailure().invoke(t -> System.out.println("msg iiiiiisssssssssss " + t.getMessage() + " and " + t))
                .onItem().ignore().andContinueWithNull();
    }

    @Override
    public Set<String> getRoles(String userId) {
        System.out.println("////////// get roles " + userId);
        if (userId.equals("admin")) {
            Set<String> ret = new HashSet<>();
            ret.add("user");
            ret.add("admin");
            return ret;
        }
        return Collections.singleton("user");
    }
}
