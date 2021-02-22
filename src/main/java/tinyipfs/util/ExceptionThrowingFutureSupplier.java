package tinyipfs.util;

import java.util.concurrent.CompletionStage;

public interface ExceptionThrowingFutureSupplier<O> {
    CompletionStage<O> get() throws Throwable;
}

