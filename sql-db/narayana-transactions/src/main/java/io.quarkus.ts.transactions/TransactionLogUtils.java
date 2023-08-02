package io.quarkus.ts.transactions;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class TransactionLogUtils {

    private TransactionLogUtils() {
        // util class
    }

    static void crashApp() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe unsafe = (Unsafe) theUnsafe.get(null);
            unsafe.copyMemory(0, 128, 256);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
