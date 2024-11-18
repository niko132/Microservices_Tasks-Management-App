package de.niko132.tasks.utils;

import android.content.Context;

import androidx.core.content.ContextCompat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

public abstract class MainFunction<T, U> implements Function<T, CompletionStage<U>> {

    private final Executor mainExecutor;

    public MainFunction(Context context) {
        this.mainExecutor = ContextCompat.getMainExecutor(context);
    }

    @Override
    public CompletionStage<U> apply(T t) {
        return CompletableFuture.supplyAsync(() -> applyOnMain(t), mainExecutor);
    }

    public abstract U applyOnMain(T t);
}
