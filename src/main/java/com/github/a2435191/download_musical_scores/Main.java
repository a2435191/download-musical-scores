package com.github.a2435191.download_musical_scores;

import com.github.a2435191.download_musical_scores.downloaders.*;
import com.github.a2435191.download_musical_scores.downloaders.implementations.GoogleDriveDownloader;
import com.github.a2435191.download_musical_scores.downloaders.implementations.MegaDownloader;
import com.github.a2435191.download_musical_scores.downloaders.implementations.SendspaceDownloader;
import com.github.a2435191.download_musical_scores.downloaders.implementations.WeTransferDownloader;
import com.github.a2435191.download_musical_scores.reddit.RedditPostInfo;
import com.github.a2435191.download_musical_scores.reddit.SubredditStream;
import com.github.a2435191.download_musical_scores.util.BadRequestStatusException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;

public final class Main {
    private Main() {
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void loopScores() throws IOException, GeneralSecurityException {
        SubredditStream sr = new SubredditStream();
        AbstractFileDownloader.DownloaderManager manager = new AbstractFileDownloader.DownloaderManager(
            new WeTransferDownloader(60),
            new GoogleDriveDownloader(60),
            new SendspaceDownloader(60),
            new MegaDownloader("", "")
        );

        while (!sr.isDone()) {
            try {
                RedditPostInfo[] data = sr.getNextPostData();
                for (RedditPostInfo info : data) {
                    System.out.println(info);
                    for (String urlString : info.scoreURLs()) {

                        AbstractFileDownloader downloader = manager.getInstanceFromUrl(URI.create(urlString));
                        if (downloader != null) {
                            try {
                                downloader.getFileTreeRoot(urlString);
                            } catch (GoogleJsonResponseException ex) {
                                if (ex.getStatusCode() != 404) {
                                    throw ex;
                                }
                            }
                        }
                    }
                }
            } catch (BadRequestStatusException ex) {
                if (ex.getResponse().statusCode() == 429) { // too many requests
                    sleep(60 * 1000);
                } else {
                    throw ex;
                }
            }
        }
    }

    public static void main(String[] args) throws Throwable {
    }
}
