package com.github.a2435191.download_musical_scores.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * Run {@link CompletableFuture}s in batches.
 *
 * @param <T> The type of the {@link CompletableFuture}.
 */
public final class JobsQueue<T> {

    private final Queue<Supplier<CompletableFutureReferenceWrapper>> waiting = new ConcurrentLinkedQueue<>();
    private final Set<CompletableFutureReferenceWrapper> running = new HashSet<>();
    private final Queue<T> output = new ArrayDeque<>();
    private final int maxJobsRunningAtOnce;

    /**
     * Initialize a new {@link JobsQueue} instance.
     *
     * @param maxJobsRunningAtOnce The maxmimum number of {@link CompletableFuture}s that will run simultaneously.
     */
    public JobsQueue(int maxJobsRunningAtOnce) {
        this.maxJobsRunningAtOnce = maxJobsRunningAtOnce;
    }

    private void recycleQueue(CompletableFutureReferenceWrapper wrapper, @Nullable T result) {
        synchronized (this) {
            if (result != null) {
                this.output.add(result);
            }

            this.running.remove(wrapper);
            if (!this.waiting.isEmpty()) {
                this.running.add(this.waiting.remove().get());
            }
            System.out.println("running : " + running + ", waiting size: " + waiting.size());
        }
    }

    /**
     * Enqueue a future.
     *
     * @param completableFutureSupplier Supplier for the future to be run.
     * @param name                      Useful for debugging.
     */
    public void add(Supplier<CompletableFuture<T>> completableFutureSupplier, @Nullable String name) {
        Supplier<CompletableFutureReferenceWrapper> wrappedSupplier = () -> {
            CompletableFutureReferenceWrapper wrapper = new CompletableFutureReferenceWrapper(null, name);
            CompletableFuture<T> originalFuture = completableFutureSupplier.get();

            @SuppressWarnings("UnnecessaryLocalVariable")
            CompletableFuture<Void> wrappedFuture = originalFuture
                .exceptionally(ex -> {
                    recycleQueue(wrapper, null);
                    throw new RuntimeException(ex);
                })
                .thenAccept(
                    result -> recycleQueue(wrapper, result)
                );
            wrapper.future = wrappedFuture;
            return wrapper;
        };
        synchronized (this) {
            if (this.running.size() < this.maxJobsRunningAtOnce) {
                this.running.add(wrappedSupplier.get());
            } else {
                this.waiting.add(wrappedSupplier);
            }
        }
    }

    /**
     * Wait for all running and waiting futures to complete before returning.
     */
    public void joinAll() {
        System.out.println("called joinAll");
        // not only must we join all futures in running, we must wait for those in waiting to trickle out as well
        while (true) {
            final boolean breakFlag;
            synchronized (this.waiting) {
                breakFlag = this.waiting.isEmpty();
            }

            final List<CompletableFutureReferenceWrapper> wrappers;
            synchronized (this.running) {
                wrappers = new ArrayList<>(this.running);
            }

            for (CompletableFutureReferenceWrapper wrapper : wrappers) {
                assert wrapper.future != null;
                try {
                    wrapper.future.join();
                } catch (CompletionException e) {
                    e.printStackTrace();
                }
            }

            if (breakFlag) {
                break;
            }
        }
    }

    /**
     * Get the results of all the completed futures so far.
     * Empties the results queue.
     *
     * @return a list of all the results of the completed futures.
     * If <code>T</code> is {@link Void}, returns an empty list.
     */
    public @NotNull List<@NotNull T> getAll() {
        synchronized (this.output) {
            List<@NotNull T> out = new ArrayList<>(this.output);
            this.output.clear();
            return out;
        }
    }


    private static class CompletableFutureReferenceWrapper {
        @Nullable CompletableFuture<Void> future;

        @SuppressWarnings("CanBeFinal")
        @Nullable String name;


        CompletableFutureReferenceWrapper(@SuppressWarnings("SameParameterValue") @Nullable CompletableFuture<Void> future, @Nullable String name) {
            this.future = future;
            this.name = name;
        }

        @Override
        public String toString() {
            return "name: " + this.name;
        }
    }

}