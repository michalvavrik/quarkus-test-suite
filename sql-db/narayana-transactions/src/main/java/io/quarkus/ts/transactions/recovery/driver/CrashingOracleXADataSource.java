package io.quarkus.ts.transactions.recovery.driver;

import java.sql.SQLException;

import javax.sql.XAConnection;
import oracle.jdbc.xa.client.OracleXADataSource;

public class CrashingOracleXADataSource extends OracleXADataSource {
    public CrashingOracleXADataSource() throws SQLException {
    }

    @Override
    public XAConnection getXAConnection() throws SQLException {
        return getXAConnection(getUser(), getPassword().get());
    }

    @Override
    public XAConnection getXAConnection(String userName, String passwd) throws SQLException {
        return new CrashingXAConnection(super.getXAConnection(userName, passwd));
    }
}
