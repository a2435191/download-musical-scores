package com.github.a2435191.download_musical_scores.reddit;

import com.github.a2435191.download_musical_scores.util.BadRequestStatusException;
import com.github.a2435191.download_musical_scores.util.HttpUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Unused
 */
@Deprecated(forRemoval = true)
public class RedditAuth {
    public static final String USER_AGENT = "Musical Scores Downloader by u/2435191";
    private static final String AUTH_URL = "https://www.reddit.com/api/v1/access_token";
    private static final String POST_INFO_URL = "https://oauth.reddit.com/api/info";
    private final String username;
    private final String password;
    private final String clientID;
    private final String clientSecret;

    public RedditAuth(String username, String password, String clientID, String clientSecret) {
        this.username = username;
        this.password = password;
        this.clientID = clientID;
        this.clientSecret = clientSecret;
    }

    public RedditAuth(Path jsonPath) throws IOException {
        JSONObject json = new JSONObject(Files.readString(jsonPath));
        this.username = json.getString("username");
        this.password = json.getString("password");
        this.clientID = json.getString("clientID");
        this.clientSecret = json.getString("clientSecret");
    }

    private @NotNull String getBasicAuthString() {
        return "Basic " + Base64.getEncoder().encodeToString(
            (this.clientID + ":" + this.clientSecret).getBytes()
        );
    }

    public void auth() throws BadRequestStatusException {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(AUTH_URL))
            .header("Authorization", getBasicAuthString())
            .header("User-Agent", USER_AGENT)
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    HttpUtils.urlEncode(
                        Map.of(
                            "grant_type", "password",
                            "username", this.username,
                            "password", this.password
                        ),
                        List.of()
                    )
                )
            )
            .build();
        HttpResponse<String> response = client
            .sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .join();
        if (response.statusCode() != 200) {
            throw new BadRequestStatusException(response);
        }
        String bearerToken = new JSONObject(response.body()).getString("access_token");

    }


}
