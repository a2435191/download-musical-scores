package com.github.a2435191.download_musical_scores.downloaders.implementations;

import com.github.a2435191.download_musical_scores.downloaders.AbstractFileDownloader;
import com.github.a2435191.download_musical_scores.filetree.AbstractFileNode;
import com.github.a2435191.download_musical_scores.filetree.URLFileNode;
import com.github.a2435191.download_musical_scores.reddit.RedditClient;
import com.github.a2435191.download_musical_scores.util.BadRequestStatusException;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DropboxDownloader extends AbstractFileDownloader {

    private static final Pattern FILENAME_REGEX = Pattern.compile("filename=\"(.+)\";");


    public DropboxDownloader() {
        super();
    }

    private static @NotNull URI createURIFromURIAndParams(@NotNull URI originalURL, Collection<NameValuePair> pairs) {
        final URI out;
        try {
            out = new URI(
                originalURL.getScheme(),
                originalURL.getUserInfo(),
                originalURL.getHost(),
                originalURL.getPort(),
                originalURL.getPath(),
                URLEncodedUtils.format(pairs, StandardCharsets.UTF_8),
                originalURL.getFragment()
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return out;
    }

    private static @NotNull URI setDownloadParamToOne(@NotNull URI originalURL) {
        boolean alreadyHasDlParameter = false;
        final List<NameValuePair> pairs = URLEncodedUtils.parse(originalURL, StandardCharsets.UTF_8);
        for (int i = 0; i < pairs.size(); i++) {
            NameValuePair pair = pairs.get(i);
            if (pair.getName().equals("dl")) {
                alreadyHasDlParameter = true;
                pairs.set(i, new BasicNameValuePair("dl", "1"));
                break;
            }
        }
        if (!alreadyHasDlParameter) {
            pairs.add(new BasicNameValuePair("dl", "1"));
        }

        return createURIFromURIAndParams(originalURL, pairs);
    }


    @Override
    public @NotNull AbstractFileNode getFileTreeRoot(@NotNull String url) {
        final URI downloadURL = setDownloadParamToOne(URI.create(url));
        final HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(downloadURL)
            .header("User-Agent", RedditClient.USER_AGENT)
            .build();

        return new URLFileNode() {
            @Override
            protected @NotNull FileInfo download() throws IOException {
                HttpResponse<InputStream> response = this.client
                    .sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .join();

                BadRequestStatusException.raiseOnStatus(response);

                Optional<String> contentDisposition = response.headers().firstValue("content-disposition");
                if (contentDisposition.isEmpty()) {
                    throw new RuntimeException("Content disposition is empty!");
                }

                Matcher matcher = FILENAME_REGEX.matcher(contentDisposition.get());
                if (!matcher.find()) {
                    throw new RuntimeException("No match found in " + contentDisposition.get());
                }
                String name = matcher.group(1);

                return new FileInfo(response.body(), name);
            }
        };

    }
}
