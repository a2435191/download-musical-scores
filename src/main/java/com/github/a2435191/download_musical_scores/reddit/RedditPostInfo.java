package com.github.a2435191.download_musical_scores.reddit;

import com.github.a2435191.download_musical_scores.util.URLTextExtractor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Represents a subset of information contained in a Reddit post's metadata.
 *
 * @param id            Base-36 unique id for the post.
 *                      See <a href="https://www.reddit.com/r/redditdev/comments/6ry178/how_to_find_a_link_to_a_comment_etc_from_the_base/">this</a>.
 * @param timestamp     Unix timestamp of post creation
 * @param redditURLPath The URL to the post
 * @param scoreURLs     Score URLs extracted from the post.
 *                      See {@link URLTextExtractor#extractURLsFromRedditPost(JSONObject, boolean)}.
 */
public record RedditPostInfo(String id, long timestamp, String redditURLPath, String[] scoreURLs) {

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return "RedditPostInfo{" +
                   "id='" + id + '\'' +
                   ", timestamp=" + timestamp +
                   ", redditURLPath='" + redditURLPath + '\'' +
                   ", scoreURLs=" + Arrays.toString(scoreURLs) +
                   '}';
    }
}
