package tinyipfs.util;

public interface ExceptionThrowingFunction<I, O> {
    O apply(I value) throws Throwable;
}

