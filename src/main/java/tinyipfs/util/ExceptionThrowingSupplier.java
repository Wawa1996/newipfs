package tinyipfs.util;

public interface ExceptionThrowingSupplier<O> {
    O get() throws Throwable;
}

