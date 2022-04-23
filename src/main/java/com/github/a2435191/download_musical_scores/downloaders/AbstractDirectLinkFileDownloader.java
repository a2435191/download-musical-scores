package com.github.a2435191.download_musical_scores.downloaders;

import java.net.http.HttpClient;
import java.time.Duration;

public abstract class AbstractDirectLinkFileDownloader extends AbstractFileDownloader {
    protected final int timeoutSeconds;
    protected final HttpClient client;

    protected AbstractDirectLinkFileDownloader(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        this.client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(timeoutSeconds))
            .build();
    }
}
