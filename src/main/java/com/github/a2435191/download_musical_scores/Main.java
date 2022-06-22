package com.github.a2435191.download_musical_scores;


import com.github.a2435191.download_musical_scores.reddit.RedditPostInfo;
import com.github.a2435191.download_musical_scores.reddit.SubredditStream;
import com.github.a2435191.download_musical_scores.util.BadRequestStatusException;
import com.github.a2435191.download_musical_scores.util.FileUtils;
import com.github.a2435191.download_musical_scores.util.JobsQueue;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class Main {


    private static final String SUBMISSION_PREFIX = "[SUBMISSION]";

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static @NotNull String escapeTitle(@NotNull String title) {
        String escapedTitle = title.replace("/", "|");

        if (escapedTitle.toUpperCase().startsWith(SUBMISSION_PREFIX)) {
            escapedTitle = escapedTitle
                .substring(SUBMISSION_PREFIX.length())
                .stripLeading();
        }
        if (escapedTitle.isEmpty()) {
            return SUBMISSION_PREFIX; // TODO: make sure no duplicates
        }
        return escapedTitle;
    }

    private static void download(MusicalScoresDownloader downloader, String url, Path targetPath) {
        try {
            downloader.download(url, targetPath);
        } catch (FileAlreadyExistsException e) {
            e.printStackTrace(System.err);
        } catch (IOException | RuntimeException e) {
            e.printStackTrace(System.err);
            try {
                System.out.println("deleting " + targetPath + " for " + url);
                if (targetPath.toFile().exists()) {
                    FileUtils.delete(targetPath);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static void zip(String url, Path targetPath) {
        System.out.println("zipping " + url);
        Path zipDownloadPath = Path.of(targetPath + ".zip");
        if (!Files.exists(targetPath)) {
            return;
        }
        try {
            FileUtils.zipDirectory(targetPath, zipDownloadPath);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        } finally {
            try {
                if (targetPath.toFile().exists()) {
                    FileUtils.delete(targetPath);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void downloadAll(boolean zip, @NotNull String subredditName, Path downloadDir) {
        MusicalScoresDownloader downloader = new MusicalScoresDownloader();
        SubredditStream stream = new SubredditStream(subredditName);

        Stream<@NotNull RedditPostInfo> infoStream = Stream.generate(() -> {
                try {
                    return stream.getNextPostData();
                } catch (BadRequestStatusException badRequestStatusException) {
                    if (badRequestStatusException.getResponse().statusCode() == 429) { // too many requests
                        System.out.println("waiting on 429");
                        sleep(60_000);
                    }
                    badRequestStatusException.printStackTrace(System.err);
                    return null;
                }
            })
            .takeWhile($ -> !stream.isDone())
            .filter(Objects::nonNull)
            .flatMap(Stream::of)
            //.limit(100)
            ; // TODO: remember to remove this


        JobsQueue<Void> queue = new JobsQueue<>(15);
        infoStream.forEach(info -> {
            final String escapedTitle = escapeTitle(info.title());
            for (String url : info.scoreURLs()) {
                Path targetPath = downloadDir.resolve(escapedTitle);

                System.out.println("downloading " + url);
                Supplier<CompletableFuture<Void>> futureSupplier = () -> {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(
                        () -> download(downloader, url, targetPath)
                    );
                    if (zip) {
                        future = future.thenRunAsync(() -> zip(url, targetPath));
                    }
                    return future;
                };
                queue.add(futureSupplier, url);
            }
        });

        queue.joinAll();

    }


    public static void main(String[] args) throws Throwable {
        downloadAll(true, "MusicalScores", Path.of("downloads"));
    }


}
