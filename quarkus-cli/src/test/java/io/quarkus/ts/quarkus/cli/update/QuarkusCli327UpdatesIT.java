package io.quarkus.ts.quarkus.cli.update;

import java.io.IOException;
import java.util.Properties;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.quarkus.test.util.QuarkusCLIUtils;

@Tag("quarkus-cli")
public class QuarkusCli327UpdatesIT extends AbstractQuarkusCliUpdateIT {

    public QuarkusCli327UpdatesIT() {
        super(new DefaultArtifactVersion("3.20"), new DefaultArtifactVersion("3.27"));
    }

    /**
     * Tests Quarkus CLI update recipe for 3.23.
     * See <a href=
     * "https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.23#schema-management-configuration-properties-gear-white_check_mark">the
     * migration guide note</a>.
     */
    @Test
    public void testHibernateSchemaPropertiesUpdated() throws IOException {
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
}
