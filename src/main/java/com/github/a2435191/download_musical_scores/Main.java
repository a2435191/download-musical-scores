package com.github.a2435191.download_musical_scores;


import com.github.a2435191.download_musical_scores.reddit.RedditPostInfo;
import com.github.a2435191.download_musical_scores.reddit.SubredditStream;
import com.github.a2435191.download_musical_scores.util.BadRequestStatusException;
import com.github.a2435191.download_musical_scores.util.FileUtils;
import com.github.a2435191.download_musical_scores.util.JobsQueue;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
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
        if (!Files.exists(targetPath)) {
            return;
        }
        System.out.println("zipping " + url);
        Path zipDownloadPath = Path.of(targetPath + ".zip");
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



    private static boolean getOverwriteFromFile(File csvFile, String id, int linkIndex) throws IOException {
        return false;
    }

    public static void downloadAll(
        @NotNull String subredditName,
        @NotNull Path downloadDir,
        @NotNull Predicate<@Nullable PersistentDownloadData> overwritePredicate,
        @NotNull BiPredicate<@NotNull RedditPostInfo, @NotNull Integer> zipPredicate,
        @NotNull File persistentDataCSV,
        @NotNull ConcurrentLinkedQueue<@NotNull PersistentDownloadData> outData)
    {
        try {
            downloadAll(subredditName, downloadDir, overwritePredicate, zipPredicate,
                PersistentDownloadData.fromCSV(persistentDataCSV),
                outData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



    public static void downloadAll(
        @NotNull String subredditName,
        @NotNull Path downloadDir,
        @NotNull Predicate<@Nullable PersistentDownloadData> overwritePredicate,
        @NotNull BiPredicate<@NotNull RedditPostInfo, @NotNull Integer> zipPredicate,
        @NotNull Map<Map.Entry<@NotNull String, @NotNull Integer>, @NotNull PersistentDownloadData> persistentDownloadDataMap,
        @NotNull ConcurrentLinkedQueue<@NotNull PersistentDownloadData> outData)
    {
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
            .limit(10)
            ;


        JobsQueue<Void> queue = new JobsQueue<>(15);
        infoStream.forEach(info -> {
            final String escapedTitle = escapeTitle(info.title());
            for (int linkNumber = 0; linkNumber < info.scoreURLs().length; linkNumber++) {
                String url = info.scoreURLs()[linkNumber];
                Path targetPath = downloadDir.resolve(escapedTitle);

                @Nullable PersistentDownloadData persistentDownloadData =
                    persistentDownloadDataMap.get(Map.entry(info.id(), linkNumber));

                final boolean overwrite = overwritePredicate.test(persistentDownloadData);
                final boolean zip = zipPredicate.test(info, linkNumber);

                final int tmp = linkNumber;


                System.out.println("downloading " + info.title() + " at " + url);
                Supplier<CompletableFuture<Void>> futureSupplier = () -> {
                    CompletableFuture<Void> future;
                    if (overwrite && Files.exists(targetPath)) {
                        future = CompletableFuture.runAsync(() -> {});
                    } else {
                        future = CompletableFuture.runAsync(() -> {
                            download(downloader, url, targetPath);
                            outData.add(
                                new PersistentDownloadData(info.id(), targetPath, tmp, // TODO confirm targetPath is always right
                                    URI.create(url), LocalDateTime.now(), false)
                            );
                        });
                    }

                    if (zip && (!overwrite || !Files.exists(Path.of(targetPath + ".zip")))) {
                        future = future.thenRunAsync(() -> {
                            zip(url, targetPath);
                            outData.add(
                                new PersistentDownloadData(info.id(), Path.of(targetPath + ".zip"), tmp,
                                    URI.create(url), LocalDateTime.now(), false)
                            );
                        });
                    }

                    return future;
                };
                queue.add(futureSupplier, url);
            }
        });

        queue.joinAll();

    }



    public static void main(String[] args) throws Throwable {
        var map = PersistentDownloadData.fromCSV(new File("downloads.csv"));
        ConcurrentLinkedQueue<PersistentDownloadData> list = new ConcurrentLinkedQueue<>();
        downloadAll(
            "MusicalScores",
            Path.of("downloads2"),
            persistentDownloadData -> persistentDownloadData == null || persistentDownloadData.overwrite(),
            (info, i) -> true,
            new File("downloads.csv"),
            list
        );
        System.out.println(list);
    }


}
