package logbook.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * スレッドを管理します
 *
 */
public final class ThreadManager {

    /** Executor */
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(4);

    /** Virtual Thread Executor */
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * アプリケーションで共有するExecutorService
     * <p>
     * 長時間実行する必要のあるスレッドを登録する場合、割り込みされたかを検知して適切に終了するようにしてください。
     * </p>
     *
     * @return ExecutorService
     */
    public static ExecutorService getExecutorService() {
        return VIRTUAL_EXECUTOR;
    }

    /**
     * スケジューリングが必要な場合に使用するExecutorService
     *
     * @return ScheduledExecutorService
     */
    public static ScheduledExecutorService getScheduledExecutorService() {
        return EXECUTOR;
    }
}
