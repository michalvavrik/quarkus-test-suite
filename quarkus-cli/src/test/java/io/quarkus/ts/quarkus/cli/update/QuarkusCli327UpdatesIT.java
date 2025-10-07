package io.quarkus.ts.quarkus.cli.update;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.bootstrap.QuarkusCliRestService;
import io.quarkus.test.util.QuarkusCLIUtils;
import io.quarkus.test.utils.FileUtils;

@Tag("quarkus-cli")
public class QuarkusCli327UpdatesIT extends AbstractQuarkusCliUpdateIT {

    public QuarkusCli327UpdatesIT() {
        super(new DefaultArtifactVersion("3.20"), new DefaultArtifactVersion("3.27"));
    }

    /**
     * Tests Quarkus CLI update recipe for 3.21.
     * See <a href=
     * "https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.21#tls-registry-gear-white_check_mark">the
     * migration guide note</a>.
     */
    @Test
    void testTlsRegistryBuildItemPackageChange() {
        QuarkusCliRestService app = quarkusCLIAppManager.createApplication();

        // we obviously don't have extension and the deployment module, but we just need to verify the update command
        Path testProcessorPath = app.getServiceFolder().resolve("src").resolve("main").resolve("java").resolve("org")
                .resolve("acme").resolve("TestProcessor.java");
        FileUtils.copyContentTo("""
                package org.acme;

                import io.quarkus.deployment.annotations.BuildStep;
                import io.quarkus.deployment.annotations.Produce;
                import io.quarkus.deployment.builditem.ServiceStartBuildItem;
                import io.quarkus.tls.TlsRegistryBuildItem;

                public class TestProcessor {

                    @Produce(ServiceStartBuildItem.class) // step is invoked if it produces or records something
                    @BuildStep
                    void useTlsRegistry(TlsRegistryBuildItem buildItem) {
                        // nothing to do
                    }

                }
                """, testProcessorPath);

        // add the TLS registry deployment module
        File pomFile = app.getFileFromApplication("pom.xml");
        String originalPomFileContent = FileUtils.loadFile(pomFile);
        String updatedPomFileContent = originalPomFileContent.replace("<artifactId>quarkus-rest</artifactId>", """
                <artifactId>quarkus-rest</artifactId>
                </dependency>
                <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-tls-registry</artifactId>
                </dependency>
                <dependency>
                    <groupId>io.quarkus</groupId>
                    <artifactId>quarkus-tls-registry-deployment</artifactId>
                """);
        FileUtils.copyContentTo(updatedPomFileContent, pomFile.toPath());

        quarkusCLIAppManager.updateApp(app);

        String updatedFileContent = FileUtils.loadFile(testProcessorPath.toFile());
        Assertions.assertThat(updatedFileContent)
                .contains("io.quarkus.tls.deployment.spi.TlsRegistryBuildItem")
                .doesNotContain("io.quarkus.tls.TlsRegistryBuildItem");
    }

    /**
     * Tests Quarkus CLI update recipes for 3.23.
     * See <a href=
     * "https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.23#schema-management-configuration-properties-gear-white_check_mark">the
     * migration guide note</a>.
     */
    @Test
    void testHibernateSchemaPropertiesUpdated() throws IOException {
        Properties oldProperties = new Properties();
        Properties newProperties = new Properties();

        //== DEFAULT PERSISTENCE UNIT

        oldProperties.put("quarkus.hibernate-orm.database.generation", "drop-and-create");
        newProperties.put("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create");

        oldProperties.put("quarkus.hibernate-orm.database.generation.create-schemas", "true");
        newProperties.put("quarkus.hibernate-orm.schema-management.create-schemas", "true");

        oldProperties.put("quarkus.hibernate-orm.database.generation.halt-on-error", "true");
        newProperties.put("quarkus.hibernate-orm.schema-management.halt-on-error", "true");

        oldProperties.put("quarkus.hibernate-orm.database.generation.halt-on-error", "true");
        newProperties.put("quarkus.hibernate-orm.schema-management.halt-on-error", "true");

        //== NAMED PERSISTENCE UNITS

        oldProperties.put("quarkus.hibernate-orm.super.database.generation", "drop-and-create");
        newProperties.put("quarkus.hibernate-orm.super.schema-management.strategy", "drop-and-create");

        oldProperties.put("quarkus.hibernate-orm.super.database.generation.create-schemas", "true");
        newProperties.put("quarkus.hibernate-orm.super.schema-management.create-schemas", "true");

        oldProperties.put("quarkus.hibernate-orm.super.database.generation.halt-on-error", "true");
        newProperties.put("quarkus.hibernate-orm.super.schema-management.halt-on-error", "true");

        oldProperties.put("quarkus.hibernate-orm.super.database.generation.halt-on-error", "true");
        newProperties.put("quarkus.hibernate-orm.super.schema-management.halt-on-error", "true");

        oldProperties.put("quarkus.hibernate-orm.\"fantastic\".database.generation", "drop-and-create");
        newProperties.put("quarkus.hibernate-orm.\"fantastic\".schema-management.strategy", "drop-and-create");

        oldProperties.put("quarkus.hibernate-orm.\"fantastic\".database.generation.create-schemas", "true");
        newProperties.put("quarkus.hibernate-orm.\"fantastic\".schema-management.create-schemas", "true");

        oldProperties.put("quarkus.hibernate-orm.\"fantastic\".database.generation.halt-on-error", "true");
        newProperties.put("quarkus.hibernate-orm.\"fantastic\".schema-management.halt-on-error", "true");

        oldProperties.put("quarkus.hibernate-orm.\"fantastic\".database.generation.halt-on-error", "true");
        newProperties.put("quarkus.hibernate-orm.\"fantastic\".schema-management.halt-on-error", "true");

        QuarkusCLIUtils.checkPropertiesUpdate(quarkusCLIAppManager, oldProperties, newProperties);
    }

    /**
     * Tests Quarkus CLI update recipes for 3.26.
     * See <a href=
     * "https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.26#enable--enabled-in-configuration-properties-gear-white_check_mark">the
     * migration guide note</a>.
     */
    @Test
    void testEnableToEnabledPropertiesRenaming() throws IOException {
        Properties oldProperties = new Properties();
        Properties newProperties = new Properties();

        oldProperties.put("quarkus.keycloak.policy-enforcer.enable", "true");
        newProperties.put("quarkus.keycloak.policy-enforcer.enabled", "true");

        oldProperties.put("quarkus.log.console.enable", "true");
        newProperties.put("quarkus.log.console.enabled", "true");

        oldProperties.put("quarkus.log.console.async.enable", "true");
        newProperties.put("quarkus.log.console.async.enabled", "true");

        oldProperties.put("quarkus.log.file.enable", "true");
        newProperties.put("quarkus.log.file.enabled", "true");

        oldProperties.put("quarkus.log.file.async.enable", "true");
        newProperties.put("quarkus.log.file.async.enabled", "true");

        oldProperties.put("quarkus.log.syslog.enable", "true");
        newProperties.put("quarkus.log.syslog.enabled", "true");

        oldProperties.put("quarkus.log.syslog.async.enable", "true");
        newProperties.put("quarkus.log.syslog.async.enabled", "true");

        oldProperties.put("quarkus.log.socket.enable", "true");
        newProperties.put("quarkus.log.socket.enabled", "true");

        oldProperties.put("quarkus.log.socket.async.enable", "true");
        newProperties.put("quarkus.log.socket.async.enabled", "true");

        oldProperties.put("quarkus.snapstart.enable", "true");
        newProperties.put("quarkus.snapstart.enabled", "true");

        oldProperties.put("quarkus.smallrye-health.ui.enable", "true");
        newProperties.put("quarkus.smallrye-health.ui.enabled", "true");

        oldProperties.put("quarkus.smallrye-graphql.ui.enable", "true");
        newProperties.put("quarkus.smallrye-graphql.ui.enabled", "true");

        oldProperties.put("quarkus.smallrye-openapi.enable", "true");
        newProperties.put("quarkus.smallrye-openapi.enabled", "true");

        oldProperties.put("quarkus.swagger-ui.enable", "true");
        newProperties.put("quarkus.swagger-ui.enabled", "true");

        oldProperties.put("quarkus.log.handler.console.whatever.enable", "true");
        newProperties.put("quarkus.log.handler.console.whatever.enabled", "true");

        oldProperties.put("quarkus.log.handler.console.\"whatever\".enable", "true");
        newProperties.put("quarkus.log.handler.console.\"whatever\".enabled", "true");

        //                - org.openrewrite.quarkus.ChangeQuarkusPropertyKey:
        //        oldPropertyKey: quarkus\.log\.handler\.console\.(.*)\.enable
        //        newPropertyKey: quarkus\.log\.handler\.console\.$1\.async\.enabled
        //                - org.openrewrite.quarkus.ChangeQuarkusPropertyKey:
        //        oldPropertyKey: quarkus\.log\.handler\.file\.(.*)\.enable
        //        newPropertyKey: quarkus\.log\.handler\.file\.$1\.enabled
        //                - org.openrewrite.quarkus.ChangeQuarkusPropertyKey:
        //        oldPropertyKey: quarkus\.log\.handler\.file\.(.*)\.enable
        //        newPropertyKey: quarkus\.log\.handler\.file\.$1\.async\.enabled
        //                - org.openrewrite.quarkus.ChangeQuarkusPropertyKey:
        //        oldPropertyKey: quarkus\.log\.handler\.syslog\.(.*)\.enable
        //        newPropertyKey: quarkus\.log\.handler\.syslog\.$1\.enabled
        //                - org.openrewrite.quarkus.ChangeQuarkusPropertyKey:
        //        oldPropertyKey: quarkus\.log\.handler\.syslog\.(.*)\.enable
        //        newPropertyKey: quarkus\.log\.handler\.syslog\.$1\.async\.enabled
        //                - org.openrewrite.quarkus.ChangeQuarkusPropertyKey:
        //        oldPropertyKey: quarkus\.log\.handler\.socket\.(.*)\.enable
        //        newPropertyKey: quarkus\.log\.handler\.socket\.$1\.enabled
        //                - org.openrewrite.quarkus.ChangeQuarkusPropertyKey:
        //        oldPropertyKey: quarkus\.log\.handler\.socket\.(.*)\.enable
        //        newPropertyKey: quarkus\.log\.handler\.socket\.$1\.async\.enabled

        QuarkusCLIUtils.checkPropertiesUpdate(quarkusCLIAppManager, oldProperties, newProperties);
    }
}
