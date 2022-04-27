package com.github.a2435191.download_musical_scores.downloaders.implementations;

import com.github.a2435191.download_musical_scores.downloaders.AbstractDirectLinkFileDownloader;
import com.github.a2435191.download_musical_scores.filetree.AbstractFileNode;
import com.github.a2435191.download_musical_scores.filetree.URLFileNode;
import com.github.a2435191.download_musical_scores.util.BadRequestStatusException;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class StackStorageDownloader extends AbstractDirectLinkFileDownloader {
    // TODO: confirm this works with folders
    public StackStorageDownloader(int timeoutSeconds) {
        super(timeoutSeconds);
    }

    private static @NotNull Map<@NotNull String, @NotNull String> getCookiesFromString(@NotNull String cookieString) {
        return Arrays.stream(cookieString
                .split("; "))
            .map(s -> {
                String[] split = s.split("=");
                if (split.length != 2) {
                    return null;
                }
                return Map.entry(split[0], split[1]);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static @NotNull String urlEncodeData(@NotNull Map<?, ?> data) {
        return data
            .entrySet()
            .stream()
            .map(entry -> {
                String key = URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8);
                String val = URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8);
                return key + "=" + val;
            })
            .collect(Collectors.joining("&"));
    }

    @Override
    public @NotNull AbstractFileNode getFileTreeRoot(@NotNull String url) throws IOException {
        // TODO: confirm this works with folders
        HttpRequest mainPageRequest = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .build();
        HttpResponse<String> htmlResponse = this.client
            .sendAsync(mainPageRequest, HttpResponse.BodyHandlers.ofString())
            .join();
        BadRequestStatusException.raiseOnStatus(htmlResponse);


        Document document = Jsoup.parse(htmlResponse.body());
        String csrfToken = document.head().selectFirst("meta[name='csrf-token']").attr("content");
        String stackShareSession = getCookiesFromString(
            htmlResponse.headers().firstValue("Set-Cookie").get())
            .get("stackShareSession");


        String data = urlEncodeData(Map.of(
            "archive", "zip",
            "all", "false",
            "CSRF-Token", csrfToken,
            "paths[]", "/"
        ));

        String downloadID = URI.create(url).getPath().split("/")[2]; // because [0] is ""
        String directDownloadURL = "https://riemer46.stackstorage.com/public-share/" + downloadID + "/download/";

        return new URLFileNode() {
            @Override
            public @NotNull FileInfo download() throws IOException {

                HttpRequest directDownloadRequest = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(data))
                    .uri(URI.create(directDownloadURL))
                    .header("Cookie", "stackShareSession=" + stackShareSession)
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .build();
                HttpResponse<InputStream> response = this.client.sendAsync(
                        directDownloadRequest,
                        HttpResponse.BodyHandlers.ofInputStream()
                    )
                    .join();

                BadRequestStatusException.raiseOnStatus(response);

                String fileName = getCookiesFromString(
                    response.headers()
                        .firstValue("Content-Disposition")
                        .get()
                )
                    .get("filename");


                return new FileInfo(response.body(), fileName);
            }
        };
    }
}
