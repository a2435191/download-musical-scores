package com.github.a2435191.download_musical_scores.reddit;

import com.github.a2435191.download_musical_scores.util.BadRequestStatusException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Class for communicating with the Reddit API
 */
public final class RedditClient {
    /**
     * Value used in the User-Agent header
     */
    public static final String USER_AGENT = "Musical Scores Downloader by u/2435191";
    private static final String URL_FORMAT_STRING = "https://www.reddit.com/r/%s/comments/%s/about.json";

    /**
     * Get a post's HTML.
     *
     * @param id            Base-36 unique id of post
     * @param subredditName Subreddit name
     * @return HTML as a UTF-8 string
     * @throws BadRequestStatusException if request is non-<code>200</code>
     * @apiNote There's a bug in the Reddit API: sometimes returns urls with doubly escaped characters
     * (so _, escaped as \_, is returned as \\_, which is actually \_ and is not a valid URI).
     * See <a href="https://www.reddit.com/r/bugs/comments/p7wk2t/the_redesign_adds_a_hidden_backslash_to_escape/">this</a>.
     */
    public static @NotNull String getPostHTML(String id, String subredditName) throws BadRequestStatusException {
        // TODO: use official Reddit API to submit multiple ids?
        String queryParams = RedditAuth.urlEncode(Map.of("raw_json", 1), List.of());
        URI aboutURL = URI.create(String.format(URL_FORMAT_STRING, subredditName, id) + "?" + queryParams);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(aboutURL)
            .header("User-Agent", USER_AGENT)
            .GET()
            .build();

        HttpResponse<String> response = client
            .sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .join();


        if (response.statusCode() != 200) {
            throw new BadRequestStatusException(response);
        }


        JSONObject data = new JSONArray(response.body())
            .getJSONObject(0)
            .getJSONObject("data")
            .getJSONArray("children")
            .getJSONObject(0)
            .getJSONObject("data");
        return data.getString("selftext_html");
    }

}
