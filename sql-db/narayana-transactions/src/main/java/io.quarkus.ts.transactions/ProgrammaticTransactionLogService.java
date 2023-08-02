package io.quarkus.ts.transactions;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.exceptions.ObjectStoreException;
import com.arjuna.ats.internal.arjuna.objectstore.jdbc.JDBCStore;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;

@ApplicationScoped
@Named("programmatic-transaction-log-svc")
public class ProgrammaticTransactionLogService implements TransactionLogService {

    @Inject
    TransferTopUpService transferTopUpService;

    @Inject
    AccountService accountService;

    @Inject
    UserTransaction userTransaction;

    @Inject
    TransactionManager tm;

    @Override
    public void queryAndCrash() {
        try {
            userTransaction.begin();
        } catch (NotSupportedException e) {
            throw new RuntimeException(e);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }

        makeWriteOperation();

        try {
            var transaction = tm.getTransaction();
            System.out.println("TRANSACTION IISSS " + transaction + " and status iiiss " + transaction.getStatus());
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
        var jdbcStoreEnvironmentBean = BeanPopulator.getDefaultInstance(ObjectStoreEnvironmentBean.class);
        String objectStoreType = jdbcStoreEnvironmentBean.getObjectStoreType();
        // This test fails if the Object Store is not set to JDBCStore
        System.out.println("expect is " + JDBCStore.class.getName() + " and is " + objectStoreType + " table prefix "
                + jdbcStoreEnvironmentBean.getTablePrefix() + " ds " + jdbcStoreEnvironmentBean.getJdbcDataSource() +
                " jdbc access " + jdbcStoreEnvironmentBean.getJdbcAccess() + " create table "
                + jdbcStoreEnvironmentBean.getCreateTable() + " os dir " + jdbcStoreEnvironmentBean.getObjectStoreDir()
                + " tx log size " + jdbcStoreEnvironmentBean.getTxLogSize() + " expose all logs as mbeans "
                + jdbcStoreEnvironmentBean.getExposeAllLogRecordsAsMBeans());

        try {
            var api = new JDBCStore(jdbcStoreEnvironmentBean);
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }

        //        QuarkusTransaction.begin(QuarkusTransaction.beginOptions().timeout(500000));
        //        makeWriteOperation();
        //        QuarkusTransaction.commit();
        try {
            Thread.sleep(Duration.ofMinutes(10).toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //        QuarkusTransaction
        //                .requiringNew()
        //                .call(() -> {
        //                    makeWriteOperation();
        //                    QuarkusTransaction.commit();
        //                    Thread.sleep(Duration.ofMinutes(10).toMillis());
        //
        //                    // unreachable statement
        //                    throw new IllegalStateException("Application was supposed to fail");
        //                });
    }

    private void makeWriteOperation() {
        transferTopUpService.addToJournal(ACCOUNT_NUMBER_FRANCISCO, ACCOUNT_NUMBER_EDUARDO, 1);
        accountService.decreaseBalance(ACCOUNT_NUMBER_FRANCISCO, 1);
    }
}
