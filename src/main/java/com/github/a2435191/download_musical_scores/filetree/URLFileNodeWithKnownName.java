package com.github.a2435191.download_musical_scores.filetree;

import com.github.a2435191.download_musical_scores.reddit.RedditClient;
import com.github.a2435191.download_musical_scores.util.BadRequestStatusException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * For the common case in which the filename is known ahead of download time.
 */
public class URLFileNodeWithKnownName extends URLFileNode {

    private final @NotNull String name;

    public URLFileNodeWithKnownName(@NotNull String name, @Nullable URI url) {
        super(url);
        this.name = name;
    }

    public URLFileNodeWithKnownName(@NotNull String name, @Nullable String url) {
        this(name, URI.create(url));
    }

    public URLFileNodeWithKnownName(@NotNull String name) {
        this.name = name;
    }

    @Override
    public @NotNull FileInfo download() throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(url)
            .header("User-Agent", RedditClient.USER_AGENT)
            .GET()
            .build();

        HttpResponse<InputStream> response = client.sendAsync(
            request,
            HttpResponse.BodyHandlers.ofInputStream()
        ).join();
        BadRequestStatusException.raiseOnStatus(response);
        return new FileInfo(response.body(), this.name);
    }

}