package com.github.a2435191.download_musical_scores.downloaders.implementations;

import com.github.a2435191.download_musical_scores.downloaders.AbstractDirectLinkFileDownloader;
import com.github.a2435191.download_musical_scores.filetree.AbstractFileNode;
import com.github.a2435191.download_musical_scores.filetree.URLFileNodeWithKnownName;
import com.github.a2435191.download_musical_scores.util.BadRequestStatusException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public final class WeTransferDownloader extends AbstractDirectLinkFileDownloader {
    private static final String URL_TO_GET_DOWNLOAD_LINK_FORMAT = "https://wetransfer.com/api/v4/transfers/%s/download";

    public WeTransferDownloader(int timeoutSeconds) {
        super(timeoutSeconds);
    }


    @Override
    public @NotNull AbstractFileNode getFileTreeRoot(@NotNull String url) throws BadRequestStatusException {
        final Document page;

        HttpRequest initialRequest = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .build();

        HttpResponse<Void> response = client.sendAsync(initialRequest, HttpResponse.BodyHandlers.discarding()).join();

        BadRequestStatusException.raiseOnStatus(response);

        String[] pathFragments = response.uri().getPath().split("/");
        String downloadCode = pathFragments[pathFragments.length - 2];
        String securityHash = pathFragments[pathFragments.length - 1];

        String urlWithDownloadLink = String.format(URL_TO_GET_DOWNLOAD_LINK_FORMAT, downloadCode);

        String postData = new JSONObject(Map.of(
            "security_hash", securityHash,
            "intent", "entire_transfer"
        )).toString();

        HttpRequest downloadLinkRequest = HttpRequest.newBuilder()
            .uri(URI.create(urlWithDownloadLink))
            .header("content-type", "application/json")
            .header("x-requested-with", "XMLHttpRequest")
            .POST(HttpRequest.BodyPublishers.ofString(postData))
            .build();


        HttpResponse<String> downloadLinkResponse = client.sendAsync(
                downloadLinkRequest,
                HttpResponse.BodyHandlers.ofString()
            )
            .join();


        BadRequestStatusException.raiseOnStatus(downloadLinkResponse);

        String directDownloadLink = new JSONObject(downloadLinkResponse.body()).getString("direct_link");
        URI downloadURL = URI.create(directDownloadLink);
        String[] urlPathArray = downloadURL.getPath().split("/");
        String fileName = urlPathArray[urlPathArray.length - 1];

        return new URLFileNodeWithKnownName(fileName, directDownloadLink);
    }
}
