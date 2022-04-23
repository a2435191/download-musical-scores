package com.github.a2435191.download_musical_scores.downloaders.implementations;

import com.github.a2435191.download_musical_scores.downloaders.AbstractDirectLinkFileDownloader;
import com.github.a2435191.download_musical_scores.filetree.AbstractFileNode;
import com.github.a2435191.download_musical_scores.filetree.URLFileNode;
import com.github.a2435191.download_musical_scores.filetree.URLFileNodeWithKnownName;
import com.github.a2435191.download_musical_scores.util.BadRequestStatusException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

public class SendspaceDownloader extends AbstractDirectLinkFileDownloader {

    private static final String SENDSPACE_API_VERSION = "1.2";
    private @Nullable String sessionToken = null;
    private @Nullable String sessionKey = null;

    public SendspaceDownloader(int timeoutSeconds) {
        super(timeoutSeconds);
    }

    private static @NotNull String md5ToLowercaseHexString(@NotNull String string) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("md5");
        byte[] hashed = md5.digest(string.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hashed);
    }

    private @NotNull Element request(@NotNull String method, @NotNull Map<?, ?> params) throws IOException {
        StringBuilder sb = new StringBuilder("http://api.sendspace.com/rest/?method=" + method);
        for (var pair : params.entrySet()) {
            sb.append("&").append(pair.getKey()).append("=").append(pair.getValue());
        }

        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(sb.toString()))
            .build();
        HttpResponse<String> response = this.client.sendAsync(
            request, HttpResponse.BodyHandlers.ofString()).join();

        BadRequestStatusException.raiseOnStatus(response);

        Document document = Jsoup.parse(response.body(), Parser.xmlParser());
        Element result = document.getElementsByTag("result").get(0);
        if (!result.attr("status").equals("ok")) {
            throw new IOException("Status is not ok: " + document);
        }
        return result;
    }

    public @NotNull SendspaceDownloader authorize(@NotNull String apiKey) throws IOException {
        Element result = this.request("auth.createtoken", Map.of(
            "api_key", apiKey,
            "api_version", SENDSPACE_API_VERSION
        ));
        this.sessionToken = result.getElementsByTag("token").get(0).text();
        return this;
    }

    public @NotNull SendspaceDownloader login(@NotNull String username, @NotNull String password) throws NoSuchAlgorithmException, IOException {
        if (this.sessionToken == null) {
            throw new IllegalStateException("sessionToken is null! Call authorize() first.");
        }

        String tokenedPassword = md5ToLowercaseHexString(this.sessionToken + md5ToLowercaseHexString(password));
        Element result = this.request("auth.login", Map.of(
            "token", this.sessionToken,
            "user_name", username,
            "tokened_password", tokenedPassword
        ));
        this.sessionKey = result.getElementsByTag("session_key").get(0).text();
        return this;
    }

    @Override
    public @NotNull AbstractFileNode getFileTreeRoot(@NotNull String url) throws IOException {
        if (this.sessionKey == null) {
            throw new IllegalStateException("sessionKey is null! Call login() first.");
        }
        Element result = this.request("download.getInfo", Map.of(
            "session_key", this.sessionKey,
            "file_id", url
        )); // fails for some reason FIXME

        Element download = result.getElementsByTag("download").get(0);

        return new URLFileNodeWithKnownName(download.attr("name"), download.attr("url"));
    }
}
