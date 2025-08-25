package io.quarkus.ts.transactions.recovery.driver;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.vertx.ext.web.RoutingContext;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class CrashingXAResource implements XAResource {

    public static final String TRANSACTION_LOGS_PATH = "/transaction-logs";
    public static final String RECOVERY_SUBPATH = "/recovery";
    private static final Logger LOG = Logger.getLogger(CrashingXAResource.class);
    private volatile InstanceHandle<RoutingContext> routingContextInstanceHandle = null;
    private final XAResource delegate;
    private final boolean isOracle;

    public CrashingXAResource(XAResource delegate, boolean isOracle) {
        this.delegate = delegate;
        this.isOracle = isOracle;
    }

    private RoutingContext getRoutingContext() {
        if (Arc.container() == null || !Arc.container().requestContext().isActive()) {
            // during transaction recovery, request context is not active
            return null;
        }

        if (routingContextInstanceHandle == null) {
            routingContextInstanceHandle = Arc.container().instance(RoutingContext.class);
        }

        return routingContextInstanceHandle.get();
    }

    private boolean shouldCrash() {
        var ctx = getRoutingContext();
        if (ctx == null) {
            return false;
        }
        return ctx.request().path().endsWith(TRANSACTION_LOGS_PATH + RECOVERY_SUBPATH);
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        if (shouldCrash()) {
            LOG.info("Crashing the system");
            Runtime.getRuntime().halt(1);
        }
        delegate.commit(xid, onePhase);
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        delegate.end(xid, flags);
    }

    @Override
    public void forget(Xid xid) throws XAException {
        delegate.forget(xid);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return delegate.getTransactionTimeout();
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        if (isOracle) {
            return false;
        }
        return delegate.isSameRM(xares);
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        if (isOracle && shouldCrash()) {
            // Force this to look like a different resource manager
            // This ensures Narayana cannot optimize to 1PC
            return XAResource.XA_OK; // NEVER return XA_RDONLY
        }
        return delegate.prepare(xid);
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        return delegate.recover(flag);
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        delegate.rollback(xid);
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return delegate.setTransactionTimeout(seconds);
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        delegate.start(xid, flags);
    }
}
