package io.quarkus.ts.transactions;

import jakarta.inject.Named;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/transaction-log")
public class TransactionLogResource {

    private final AnnotationTransactionLogService annotationTransactionLogSvc;
    private final TransactionLogService programmaticTransactionLogSvc;
    private final WhatEverService whatEverService;

    public TransactionLogResource(
            @Named("annotation-transaction-log-svc") AnnotationTransactionLogService annotationTransactionLogSvc,
            @Named("programmatic-transaction-log-svc") TransactionLogService programmaticTransactionLogSvc,
            WhatEverService whatEverService) {
        this.annotationTransactionLogSvc = annotationTransactionLogSvc;
        this.programmaticTransactionLogSvc = programmaticTransactionLogSvc;
        this.whatEverService = whatEverService;
    }

    @GET
    @Path("/crashed")
    public boolean crashed() {
        TransactionLogUtils.crashApp();
        return false;
    }

    @GET
    @Path("/not-crashed")
    public boolean notCrashed() {
        return true;
    }

    @GET
    @Path("/crash-with-transaction")
    public boolean crashWithTransaction() throws HeuristicRollbackException, SystemException, HeuristicMixedException,
            NotSupportedException, RollbackException {
        whatEverService.createCopyTrigger();
        whatEverService.transactional();
        //        annotationTransactionLogSvc.queryAndCrash();
        return true;
    }

}
