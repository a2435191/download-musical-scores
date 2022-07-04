package com.github.a2435191.download_musical_scores.reddit;

import com.github.a2435191.download_musical_scores.util.URLTextExtractor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents a subset of information contained in a Reddit post's metadata.
 *
 * @param id            Base-36 unique id for the post.
 *                      See <a href="https://www.reddit.com/r/redditdev/comments/6ry178/how_to_find_a_link_to_a_comment_etc_from_the_base/">this</a>.
 * @param timestamp     Unix timestamp of post creation
 * @param redditURLPath The URL to the post
 * @param scoreURLs     Score URLs extracted from the post.
 *                      See {@link URLTextExtractor#extractURLsFromRedditPost(JSONObject, String, boolean)}.
 */
public record RedditPostInfo(String id, long timestamp, String redditURLPath, String title, String[] scoreURLs,
                             Map<String, ?> otherData) {

    public RedditPostInfo(String id, long timestamp, String redditURLPath, String title, String[] scoreURLs) {
        this(id, timestamp, redditURLPath, title, scoreURLs, Map.of());
    }

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        String otherData = this.otherData.entrySet().stream()
            .map(pair -> pair.getKey() + "=" + pair.getValue())
            .collect(Collectors.joining(","));
        return "RedditPostInfo{" +
                   "id='" + id + '\'' +
                   ", timestamp=" + timestamp +
                   ", redditURLPath='" + redditURLPath + '\'' +
                   ", title='" + title + '\'' +
                   ", scoreURLs=" + Arrays.toString(scoreURLs) +
                   ", " + otherData +
                   '}';
    }
}
