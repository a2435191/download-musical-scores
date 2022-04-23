package com.github.a2435191.download_musical_scores.reddit;

import com.github.a2435191.download_musical_scores.util.BadRequestStatusException;
import com.github.a2435191.download_musical_scores.util.URLTextExtractor;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Stream {@link RedditPostInfo} data from the PushShift API.
 */
public class SubredditStream {
    public final static String SUBREDDIT = "MusicalScores";
    private static final String USER_AGENT = "r/MusicalScores Downloader by u/2435191";
    private static final String PUSHSHIFT_URL = "https://api.pushshift.io/reddit/search/submission";
    private static final String OPT_OUT_STRING = "!BotOptOut".toUpperCase();

    private static final String[] FIELDS = new String[]{
        "permalink",
        "link_flair_text",
        "id",
        "created_utc",
        "url",
        "is_self",
        "selftext",
        "title"
    };

    private final HttpClient client = HttpClient.newHttpClient();
    private Long beforeTimestamp = null;
    private boolean isDone = false;

    private static @NotNull URI createUriWithQueryParams(@NotNull String urlBase, @NotNull Map<String, String> params) {
        String url = urlBase + "?" + params
            .entrySet()
            .stream()
            .map(pair -> pair.getKey() + "=" + pair.getValue())
            .collect(Collectors.joining("&"));
        return URI.create(url);
    }


    private static boolean jsonDataIsValid(JSONObject data) {
        for (String fieldName : FIELDS) {
            if (fieldName.equals("selftext") && data.has("is_self") && !data.getBoolean("is_self")) {
                continue; // if not self-post selftext can be null, so next
            }
            if (fieldName.equals("link_flair_text")) {
                continue; // don't care about this one
            }
            // otherwise...
            if (!data.has(fieldName)) {
                return false;
            }
        }
        return data.optString("link_flair_text").equals("Submission")
                   && !data.getString("title").toUpperCase().contains(OPT_OUT_STRING)
                   && !data.optString("selftext").toUpperCase().contains(OPT_OUT_STRING);
    }


    /**
     * Convenience method for if this instance is exhausted.
     *
     * @return if last result from {@link #getNextPostData()} is empty.
     */
    public final boolean isDone() {
        return isDone;
    }

    /**
     * Reset the state of <code>this</code>, so that the newest results start being returned
     * (still in time-decreasing order).
     */
    public void reset() {
        this.beforeTimestamp = null;
    }

    /**
     * Calls {@link #getNextPostData(int, int)} with arguments <code>50</code> <code>60</code>.
     *
     * @return Array of {@link RedditPostInfo} instances representing Reddit posts in time-decreasing order.
     * @throws BadRequestStatusException if a request's status code is not <code>200</code>
     */
    public RedditPostInfo[] getNextPostData() throws BadRequestStatusException {
        return getNextPostData(50, 60);
    }

    private @NotNull HttpResponse<String> makeRequest(int maxBatchSize, int timeoutSeconds) throws BadRequestStatusException {
        Map<String, String> queryParams = new HashMap<>(Map.of(
            "subreddit", SUBREDDIT,
            "fields", String.join(",", FIELDS),
            "title:not", "request",
            "size", "" + maxBatchSize
        ));

        if (beforeTimestamp != null) {
            queryParams.put("before", "" + beforeTimestamp);
        }

        final URI target = createUriWithQueryParams(PUSHSHIFT_URL, queryParams);

        final HttpRequest request = HttpRequest.newBuilder()
            .uri(target)
            .header("User-Agent", USER_AGENT)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .GET()
            .build();

        HttpResponse<String> resp;

        resp = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join();


        if (resp.statusCode() != 200) {
            throw new BadRequestStatusException(resp);
        }
        return resp;
    }

    /**
     * Returns a page of results and advances the state of <code>this</code>.
     *
     * @param maxBatchSize   max length of the returned array (page size)
     * @param timeoutSeconds max seconds to wait on HTTP
     * @return Array of {@link RedditPostInfo} instances representing Reddit posts in time-decreasing order.
     * @throws BadRequestStatusException if a request's status code is not <code>200</code>.
     *                                   In this case, the state is not advanced.
     */
    public RedditPostInfo[] getNextPostData(int maxBatchSize, int timeoutSeconds) throws BadRequestStatusException {
        HttpResponse<String> response = this.makeRequest(maxBatchSize, timeoutSeconds);
        JSONArray data = new JSONObject(response.body()).getJSONArray("data");


        ArrayList<RedditPostInfo> infoArrayList = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {

            JSONObject postData = data.getJSONObject(i);
            if (jsonDataIsValid(postData)) {
                String url = postData.getString("permalink");
                String id = postData.getString("id");
                long timestamp = postData.getInt("created_utc");

                infoArrayList.add(
                    new RedditPostInfo(
                        id, timestamp, url, URLTextExtractor.extractURLsFromRedditPost(postData)
                    )
                );
            }
        }

        if (infoArrayList.isEmpty()) {
            this.isDone = true;
        } else {
            this.beforeTimestamp = infoArrayList.get(infoArrayList.size() - 1).timestamp() - 1;
        }

        return infoArrayList.toArray(new RedditPostInfo[0]);
    }
}
