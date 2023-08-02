package io.quarkus.ts.transactions;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;

public interface TransactionLogService {

    String ACCOUNT_NUMBER_EDUARDO = "ES8521006742088984966899";
    String ACCOUNT_NUMBER_FRANCISCO = "ES8521006742088984966817";

    void queryAndCrash() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
            RollbackException;

}
