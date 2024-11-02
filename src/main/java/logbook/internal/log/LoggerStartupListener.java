package logbook.internal.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;
import logbook.internal.AppPath;

import java.nio.file.Paths;

public class LoggerStartupListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {

    private boolean started = false;

    @Override
    public void start() {
        if (this.started) {
            return;
        }

        Context context = this.getContext();
        String logDir = Paths.get(AppPath.DATA_DIR, "logs").toAbsolutePath().toString();
        context.putProperty("LOG_DIR", logDir);

        started = true;
    }

    @Override
    public void stop() {
        this.started = false;
    }

    @Override
    public boolean isStarted() {
        return this.started;
    }

    @Override
    public boolean isResetResistant() {
        return true;
    }

    @Override
    public void onReset(LoggerContext context) {
    }

    @Override
    public void onStart(LoggerContext context) {
    }

    @Override
    public void onStop(LoggerContext context) {
    }

    @Override
    public void onLevelChange(ch.qos.logback.classic.Logger logger, ch.qos.logback.classic.Level level) {
    }
}
