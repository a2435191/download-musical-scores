package com.github.a2435191.download_musical_scores;

import com.github.a2435191.download_musical_scores.reddit.RedditPostInfo;
import com.github.a2435191.download_musical_scores.reddit.SubredditStream;
import com.github.a2435191.download_musical_scores.util.BadRequestStatusException;
import com.github.a2435191.download_musical_scores.util.FileUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class Main {


    private static final String SUBMISSION_PREFIX = "[SUBMISSION]";

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        final Path DOWNLOAD_DIR = Path.of("downloads");
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        MusicalScoresDownloader downloader = new MusicalScoresDownloader();
        SubredditStream stream = new SubredditStream();
        while (!stream.isDone()) {
            System.out.println("HERE1");
            try {
                RedditPostInfo[] dataArray = stream.getNextPostData();
                for (RedditPostInfo info : dataArray) {
                    for (String url : info.scoreURLs()) {
                        String escapedTitle = info.title().replace("/", "|");
                        if (escapedTitle.toUpperCase().startsWith(SUBMISSION_PREFIX)) {
                            escapedTitle = escapedTitle
                                .substring(SUBMISSION_PREFIX.length())
                                .stripLeading();
                        }

                        Path targetPath = DOWNLOAD_DIR.resolve(escapedTitle);

                        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                            try {
                                System.out.println("Downloading url " + url);
                                downloader.download(url, targetPath);
                                System.out.println(url + " done at " + targetPath);
                            } catch (IOException | RuntimeException e) {
                                System.err.println("error downloading " + url);
                                e.printStackTrace(System.err);
                                FileUtils.delete(targetPath.toFile());
                            }
                            System.out.println("done!");
                        });
                        futures.add(future);
                    }
                }
            } catch (BadRequestStatusException badRequestStatusException) {
                if (badRequestStatusException.getResponse().statusCode() == 429) { // too many requests
                    System.out.println("WAITING");
                    sleep(60_000);
                }
                badRequestStatusException.printStackTrace(System.err);
            }
        }

        System.out.println("HERE2");

        for (CompletableFuture<Void> future : futures) {
            future.join();
        }


    }





}
