package io.quarkus.ts.transactions;

import java.sql.SQLException;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;

import io.agroal.api.AgroalDataSource;

@ApplicationScoped
public class WhatEverService {

    @Named("transaction-log")
    @Inject
    AgroalDataSource dataSource;

    @Named("second-pg")
    @Inject
    AgroalDataSource dataSource1;

    @Named("object-store")
    @Inject
    AgroalDataSource dataSourceOS;

    @Transactional
    public void createCopyTrigger() {
        updateStatement("CREATE TABLE quarkus_jbosststxtable_historical_data AS SELECT * FROM quarkus_jbosststxtable");
        updateStatement("CREATE OR REPLACE FUNCTION object_store_historical_data() RETURNS trigger AS $emp_stamp$\n" +
                "    BEGIN\n" +
                "        INSERT INTO quarkus_jbosststxtable_historical_data(statetype, hidden, typename, uidstring, objectstate) VALUES (NEW.statetype, NEW.hidden, NEW.typename, NEW.uidstring, NEW.objectstate);\n"
                +
                "        RETURN NEW;\n" +
                "    END;\n" +
                "$emp_stamp$ LANGUAGE plpgsql;");
        updateStatement(
                "CREATE TRIGGER historical_data BEFORE insert ON quarkus_jbosststxtable FOR EACH ROW EXECUTE FUNCTION object_store_historical_data();");
    }

    private void updateStatement(String statement) {
        try (var con = dataSourceOS.getConnection()) {
            try (var st = con.createStatement()) {
                var es = st.executeUpdate(statement);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public long transactional() {
        //        return transaction(dataSource, () -> transaction(dataSource1, null));
        try (var con = dataSource.getConnection()) {
            try (var st = con.createStatement()) {
                var es = st.executeUpdate("CREATE TABLE roro0 (bla CHAR)");
                try (var con1 = dataSource1.getConnection()) {
                    try (var st1 = con1.createStatement()) {
                        var es1 = st1.executeUpdate("CREATE TABLE roro1 (bla CHAR)");
                        return es + es1;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static long transaction(AgroalDataSource dataSource, Supplier<Long> nested) {
        try (var con = dataSource.getConnection()) {
            try (var st = con.createStatement()) {
                var es = st.executeUpdate("CREATE TABLE roro (bla CHAR)");
                if (nested == null) {
                    //                    TransactionLogUtils.crashApp();
                    return es;
                } else {
                    return nested.get() + es;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
