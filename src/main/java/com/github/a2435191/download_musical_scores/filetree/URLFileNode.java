package com.github.a2435191.download_musical_scores.filetree;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;

public abstract class URLFileNode extends AbstractFileNodeStreamDownloader {

    /**
     * Client to make HTTP requests with
     */
    protected final HttpClient client = HttpClient.newHttpClient();


    protected final @Nullable URI url;

    public URLFileNode(@Nullable URI url) {
        this.url = url;
    }

    public URLFileNode(@Nullable String url) {
        if (url == null) {
            this.url = null;
        } else {
            this.url = URI.create(url);
        }
    }

    public URLFileNode() {
        this.url = null;
    }

}
