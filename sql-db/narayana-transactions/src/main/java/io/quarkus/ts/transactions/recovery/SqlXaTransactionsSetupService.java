package io.quarkus.ts.transactions.recovery;

import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.arjuna.ats.jta.xa.XidImple;

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.runtime.StartupEvent;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import oracle.jdbc.xa.client.OracleXADataSource;

@IfBuildProperty(name = "quarkus.datasource.oracle-configure-xa.db-kind", stringValue = "oracle")
public class SqlXaTransactionsSetupService {

    @Named("xa-ds-1")
    @Inject
    DataSource xaDataSource1;

    @Named("xa-ds-2")
    @Inject
    DataSource xaDataSource2;

    private static final String[] ORACLE_SQL_COMMANDS = new String[] {
            // content of $ORACLE_HOME/rdbms/admin/xaview.sql
            "@xaview.sql",
            "DROP SYNONYM v$xatrans$",
            "DROP SYNONYM v$pending_xatrans$",
            "DROP VIEW d$xatrans$",
            "DROP VIEW d$pending_xatrans$",
            "CREATE VIEW d$pending_xatrans$ AS\n" +
                    "(SELECT global_tran_fmt, global_foreign_id, branch_id\n" +
                    "   FROM   sys.pending_trans$ tran, sys.pending_sessions$ sess\n" +
                    "   WHERE  tran.local_tran_id = sess.local_tran_id\n" +
                    "     AND    tran.state != 'collecting'\n" +
                    "     AND    BITAND(TO_NUMBER(tran.session_vector),\n" +
                    "                   POWER(2, (sess.session_id - 1))) = sess.session_id)",
            "create synonym v$pending_xatrans$ for d$pending_xatrans$",
            "CREATE VIEW d$xatrans$ AS\n" +
                    "(((SELECT k2gtifmt, k2gtitid_ext, k2gtibid\n" +
                    "   FROM x$k2gte2\n" +
                    "   WHERE  k2gterct=k2gtdpct)\n" +
                    " MINUS\n" +
                    "  SELECT global_tran_fmt, global_foreign_id, branch_id\n" +
                    "   FROM   d$pending_xatrans$)\n" +
                    "UNION\n" +
                    " SELECT global_tran_fmt, global_foreign_id, branch_id\n" +
                    "   FROM   d$pending_xatrans$)",
            "create synonym v$xatrans$ for d$xatrans$",
            // https://access.redhat.com/documentation/zh-cn/red_hat_jboss_operations_network/3.2/html/installation_guide/database-oracle
            "GRANT SELECT ON sys.dba_pending_transactions TO myuser",
            "GRANT SELECT ON sys.pending_trans$ TO myuser",
            "GRANT SELECT ON sys.dba_2pc_pending TO myuser",
            "GRANT EXECUTE ON sys.dbms_xa TO myuser",
            "GRANT FORCE ANY TRANSACTION TO myuser",
            "GRANT SELECT ON DBA_PENDING_TRANSACTIONS TO myuser",
            "GRANT READ ON DBA_PENDING_TRANSACTIONS TO myuser",
            "grant select on v$xatrans$ to myuser",
            "grant select on pending_trans$ to myuser",
            "grant select on dba_2pc_pending to myuser",
            "grant execute on dbms_system to myuser"
    };

    @Inject
    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String conStr;

    public void setupSqlXaTransactions(@Observes StartupEvent event, @Named("oracle-configure-xa") DataSource dataSource)
            throws SQLException, XAException {
        System.out.println("sql executing");
        for (String oracleSqlCommand : ORACLE_SQL_COMMANDS) {
            try (var connection = dataSource.getConnection()) {
                try (var statement = connection.createStatement()) {
                    int res = statement.executeUpdate(oracleSqlCommand);
                    print(res);
                }
            } catch (SQLException e) {
                System.out.println("error is " + e.getMessage());
            }
        }

        testXaTransactionsWorks();
    }

    private void testXaTransactionsWorks() throws SQLException, XAException {
        try {

            // Prepare a statement to create the table
            var conna = xaDataSource1.getConnection();
            var stmta = conna.createStatement();

            // Prepare a statement to create the table
            var connb = xaDataSource2.getConnection();
            var stmtb = connb.createStatement();

            OracleXADataSource oxds1 = new OracleXADataSource();
            oxds1.setURL(conStr);
            oxds1.setUser("myuser");
            oxds1.setPassword("user");

            OracleXADataSource oxds2 = new OracleXADataSource();

            oxds2.setURL(conStr);
            oxds2.setUser("myuser");
            oxds2.setPassword("user");

            // Get XA connections to the underlying data sources
            XAConnection pc1 = oxds1.getXAConnection();
            XAConnection pc2 = oxds2.getXAConnection();

            // Get the physical connections
            var conn1 = pc1.getConnection();
            var conn2 = pc2.getConnection();

            // Get the XA resources
            XAResource oxar1 = pc1.getXAResource();
            XAResource oxar2 = pc2.getXAResource();

            // Create the Xids With the Same Global Ids
            Xid xid1 = new XidImple();
            Xid xid2 = new XidImple();

            // Start the Resources
            oxar1.start(xid1, XAResource.TMNOFLAGS);
            oxar2.start(xid2, XAResource.TMNOFLAGS);

            // Execute SQL operations with conn1 and conn2
            try (var statement = conn1.createStatement()) {
                int pk = 567;
                var result = statement.executeUpdate("INSERT INTO recovery_log (id) VALUES (" + pk + ")");
            }
            try (var statement = conn2.createStatement()) {
                int pk = 5657;
                var result = statement.executeUpdate("INSERT INTO recovery_log (id) VALUES (" + pk + ")");
            }

            // END both the branches -- IMPORTANT
            oxar1.end(xid1, XAResource.TMSUCCESS);
            oxar2.end(xid2, XAResource.TMSUCCESS);

            // Prepare the RMs
            int prp1 = oxar1.prepare(xid1);
            int prp2 = oxar2.prepare(xid2);

            System.out.println("Return value of prepare 1 is " + prp1);
            System.out.println("Return value of prepare 2 is " + prp2);

            boolean do_commit = true;

            if (!((prp1 == XAResource.XA_OK) || (prp1 == XAResource.XA_RDONLY)))
                do_commit = false;

            if (!((prp2 == XAResource.XA_OK) || (prp2 == XAResource.XA_RDONLY)))
                do_commit = false;

            System.out.println("do_commit is " + do_commit);
            System.out.println("Is oxar1 same as oxar2 ? " + oxar1.isSameRM(oxar2));

            if (prp1 == XAResource.XA_OK)
                if (do_commit)
                    oxar1.commit(xid1, false);
                else
                    oxar1.rollback(xid1);

            if (prp2 == XAResource.XA_OK)
                if (do_commit)
                    oxar2.commit(xid2, false);
                else
                    oxar2.rollback(xid2);

            // Close connections
            conn1.close();
            conn1 = null;
            conn2.close();
            conn2 = null;

            pc1.close();
            pc1 = null;
            pc2.close();
            pc2 = null;

            ResultSet rset = stmta.executeQuery("select * from recovery_log");
            while (rset.next())
                System.out.println("Col1 is " + rset.getInt(1));

            rset.close();
            rset = null;

            rset = stmtb.executeQuery("select * from recovery_log");
            while (rset.next())
                System.out.println("Col1 is " + rset.getString(1));

            rset.close();
            rset = null;

            stmta.close();
            stmta = null;
            stmtb.close();
            stmtb = null;

            conna.close();
            conna = null;
            connb.close();
            connb = null;

        } catch (Exception sqe) {
            sqe.printStackTrace();
        }
    }

    private static void print(int s) {
        System.out.println("ppppppp res " + s);
    }

}
