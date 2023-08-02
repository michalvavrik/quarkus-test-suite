package io.quarkus.ts.transactions;

import static io.quarkus.ts.transactions.TransactionLogUtils.crashApp;

import java.sql.SQLException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import org.apache.activemq.artemis.jms.client.ActiveMQXAConnectionFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.agroal.api.AgroalDataSource;

@ApplicationScoped
@Named("annotation-transaction-log-svc")
public class AnnotationTransactionLogService implements TransactionLogService {

    @Inject
    TransactionManager transactionManager;

    @Inject
    @Named("transaction-log")
    AgroalDataSource logDataSource;

    @Inject
    @Named("transaction-log1")
    AgroalDataSource logDataSource1;

    @Inject
    @Named("second-pg")
    AgroalDataSource secondPgDs;

    @Inject
    @ConfigProperty(name = "quarkus.artemis.url")
    String artemisUrl;

    @Inject
    @ConfigProperty(name = "quarkus.artemis.password")
    String artemisPwd;

    @Inject
    @ConfigProperty(name = "quarkus.artemis.username")
    String artemisUsername;

    @Override
    public void queryAndCrash() throws SystemException, NotSupportedException, HeuristicRollbackException,
            HeuristicMixedException, RollbackException {
        transactionManager.begin();
        var transaction = transactionManager.getTransaction();
        var firstRes = AccountEntity.<AccountEntity> findAll().firstResult();
        System.out.println("first res " + firstRes.getName());

        try (var con = logDataSource.getConnection()) {
            try (var st = con.createStatement()) {
                var res = st.executeUpdate(
                        "INSERT INTO savings (id, name, lastName, accountNumber, amount, updatedAt, createdAt) VALUES (nextval('savings_seq'), 'OsalicraG', 'Vega la de', 'CZ9250512252717368965555', -956, null, CURRENT_TIMESTAMP)");
                System.out.println("before 2nd con: " + res);
                try (var con1 = logDataSource1.getConnection()) {
                    try (var st1 = con1.createStatement()) {
                        var res1 = st1.executeUpdate(
                                "INSERT INTO savings (id, name, lastName, accountNumber, amount, updatedAt, createdAt) VALUES (nextval('savings_seq'), 'OsalicraG1', 'Vega la de1', 'CZ9250512252717368964444', -955, null, CURRENT_TIMESTAMP)");
                        System.out.println("before AMQ: " + res1);

                        try (ActiveMQXAConnectionFactory connectionFactory = new ActiveMQXAConnectionFactory(artemisUrl,
                                artemisUsername, artemisPwd)) {
                            try (JMSContext context = connectionFactory.createXAContext()) {
                                context.createProducer().send(context.createQueue("prices"),
                                        Integer.toString((int) Math.random()));
                                try (var context1 = connectionFactory.createXAContext();
                                        JMSConsumer consumer = context1.createConsumer(context.createQueue("prices"))) {
                                    transaction.enlistResource(context1.getXAResource());
                                    Message message = consumer.receive();
                                    System.out.println("AMQ msg is " + message);

                                    System.out.println("before using 2nd PG db");
                                    try (var con2nd = secondPgDs.getConnection()) {
                                        try (var st2ndpg = con2nd.createStatement()) {
                                            var res2ndpg = st2ndpg
                                                    .executeUpdate("CREATE TABLE Whatever (names char(5) not null)");
                                            System.out.println("create table with 2nd pg db: " + res2ndpg);
                                        }
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                                crashApp();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        transactionManager.commit();
    }

}
