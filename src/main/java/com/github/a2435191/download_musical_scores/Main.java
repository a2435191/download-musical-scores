package com.github.a2435191.download_musical_scores;

import com.github.a2435191.download_musical_scores.reddit.RedditPostInfo;
import com.github.a2435191.download_musical_scores.reddit.SubredditStream;
import com.github.a2435191.download_musical_scores.util.BadRequestStatusException;
import com.github.a2435191.download_musical_scores.util.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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

    private static void downloadAll() {
        final Path DOWNLOAD_DIR = Path.of("downloads");

        MusicalScoresDownloader downloader = new MusicalScoresDownloader();
        SubredditStream stream = new SubredditStream();

        Stream<@NotNull RedditPostInfo> infoStream = Stream.generate(() -> {
                try {
                    System.out.println("got");
                    return stream.getNextPostData();
                } catch (BadRequestStatusException badRequestStatusException) {
                    if (badRequestStatusException.getResponse().statusCode() == 429) { // too many requests
                        System.out.println("WAITING");
                        sleep(60_000);
                    }
                    badRequestStatusException.printStackTrace(System.err);
                    return null;
                }
            })
            .takeWhile($ -> !stream.isDone())
            .filter(Objects::nonNull)
            .flatMap(Stream::of).limit(50);

        Stream<@NotNull CompletableFuture<Void>> futures = infoStream.map(info -> {
                List<CompletableFuture<Void>> tmpFutures = new ArrayList<>();
                for (String url : info.scoreURLs()) {
                    String escapedTitle = info.title().replace("/", "|");
                    if (escapedTitle.toUpperCase().startsWith(SUBMISSION_PREFIX)) {
                        escapedTitle = escapedTitle
                            .substring(SUBMISSION_PREFIX.length())
                            .stripLeading();
                    }

                    Path targetPath = DOWNLOAD_DIR.resolve(escapedTitle);


                    System.out.println("downloading " + url);
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            System.out.println("Downloading url " + url);
                            downloader.download(url, targetPath);
                            System.out.println(url + " done at " + targetPath);
                        } catch (FileAlreadyExistsException e) {
                            e.printStackTrace(System.err);
                        } catch (IOException | RuntimeException e) {
                            StringBuilder builder = new StringBuilder("error downloading " + url + ": \n");
                            for (StackTraceElement elem : e.getStackTrace()) {
                                builder.append(elem.toString());
                            }
                            String stackTrace = builder.toString();

                            System.err.println(stackTrace);
                            //e.printStackTrace(System.err);
                            FileUtils.delete(targetPath.toFile());
                            return;
                        }
                        System.out.println("done!");

                    });
                    tmpFutures.add(future);
                }
                return tmpFutures;
            })
            .flatMap(Collection::stream);


        List<@NotNull CompletableFuture<Void>> futuresList = futures.toList();
        for (@NotNull CompletableFuture<Void> future : futuresList) {
            System.out.println("joining");
            future.join();
        }
    }

    public static void main(String[] args) {
        downloadAll();
    }


}
