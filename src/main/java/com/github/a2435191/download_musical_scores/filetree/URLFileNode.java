package com.github.a2435191.download_musical_scores.filetree;

import com.github.a2435191.download_musical_scores.reddit.RedditClient;
import com.github.a2435191.download_musical_scores.util.BadRequestStatusException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

public abstract class URLFileNode extends AbstractFileNode {

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
