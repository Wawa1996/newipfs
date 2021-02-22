package tinyipfs.example.libp2p.discovery;

import org.hyperledger.besu.plugin.services.MetricsSystem;

import java.util.concurrent.*;

public class MetricTrackingExecutorFactory {

    private final MetricsSystem metricsSystem;

    public MetricTrackingExecutorFactory(final MetricsSystem metricsSystem) {
        this.metricsSystem = metricsSystem;
    }

    /**
     * Creates a new {@link ExecutorService} which creates up to {@code maxThreads} threads as needed
     * and when all threads are busy, queues up to {@code maxQueueSize} further tasks to execute when
     * threads are available. When the maximum thread count and queue size are reached tasks are
     * rejected with {@link java.util.concurrent.RejectedExecutionException}
     *
     * <p>Metrics about the number of threads and queue length are automatically captured.
     *
     * @param name the name to use as a prefix in metric names. Must be unique.
     * @param maxThreads the maximum number of threads to run at any one time.
     * @param maxQueueSize the maximum capacity of the pending task queue.
     * @param threadFactory the thread factory to use when creating threads.
     * @return the new {@link ExecutorService}
     */
    public ExecutorService newCachedThreadPool(
            final String name,
            final int maxThreads,
            final int maxQueueSize,
            final ThreadFactory threadFactory) {

        // ThreadPoolExecutor has a weird API. maximumThreadCount only applies if you use a
        // SynchronousQueue but then tasks are rejected once max threads are reached instead of being
        // queued. So we use a blocking queue to ensure there is some limit on the queue size but that
        // means that the maximum number of threads is ignored and only the core thread pool size is
        // used. So, we set maximum and core thread pool to the same value and allow core threads to
        // time out and exit if they are unused.

        final ThreadPoolExecutor executor =
                new ThreadPoolExecutor(
                        maxThreads,
                        maxThreads,
                        60,
                        TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(maxQueueSize),
                        threadFactory);
        executor.allowCoreThreadTimeOut(true);

        metricsSystem.createIntegerGauge(
                TekuMetricCategory.EXECUTOR,
                name + "_queue_size",
                "Current size of the executor task queue",
                () -> executor.getQueue().size());
        metricsSystem.createIntegerGauge(
                TekuMetricCategory.EXECUTOR,
                name + "_thread_pool_size",
                "Current number of threads in the executor thread pool",
                executor::getPoolSize);
        metricsSystem.createIntegerGauge(
                TekuMetricCategory.EXECUTOR,
                name + "_thread_active_count",
                "Current number of threads executing tasks for this executor",
                executor::getActiveCount);

        return executor;
    }
}
