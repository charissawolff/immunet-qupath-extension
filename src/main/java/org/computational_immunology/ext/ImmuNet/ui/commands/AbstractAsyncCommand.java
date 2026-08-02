package org.computational_immunology.ext.ImmuNet.ui.commands;

import java.util.function.Consumer;

import org.computational_immunology.ext.ImmuNet.core.ImmuNetLog;

import javafx.concurrent.Task;

public abstract class AbstractAsyncCommand<T> {
    protected Runnable onDone;
    protected Runnable onFailed;
    protected Runnable onCancelled;
    protected Task<T> task;
    
    public void setOnDone(Runnable onDone) {
        this.onDone = onDone;
    }

    public void setOnFailed(Runnable onFailed) {
        this.onFailed = onFailed;
    }

    public void setOnCancelled(Runnable onCancelled) {
        this.onCancelled = onCancelled;
    }

    protected void onSuccess(T result) {
        // override when a command needs to act on its result
    }

    protected void onCancellation() {
        // override when a command needs to act on its result
        ImmuNetLog.log("Cancelled: " + getThreadName());
    }

    protected void onFailure(Throwable exception) {
        // override when a command needs to act on its result
        ImmuNetLog.error("Failed: " + getThreadName(), exception);
    }

    public Task<T> getTask() {
        return task;
    }

    public void build() {
        task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return execute(message -> updateMessage(message));
            }
        };
    }

    public void start() {
        task.setOnSucceeded(event -> {
            T result = task.getValue();
            onSuccess(result);
            if (onDone != null) {
                onDone.run();
            }
        });
        task.setOnCancelled(event -> {
            onCancellation();
            if (onCancelled != null) {
                onCancelled.run();
            }
        });
        task.setOnFailed(event -> {
            onFailure(task.getException());
            if (onFailed != null) {
                onFailed.run();
            }
        });
        Thread thread = new Thread(task, getThreadName());
        thread.setDaemon(true);
        thread.start();
    }

    protected abstract T execute(Consumer<String> progressReporter) throws Exception;

    protected String getThreadName() {
        return getClass().getSimpleName();
    }
}