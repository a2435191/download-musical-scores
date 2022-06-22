package com.github.a2435191.download_musical_scores.util;

import com.github.a2435191.download_musical_scores.reddit.RedditClient;
import org.apache.commons.validator.routines.UrlValidator;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@SuppressWarnings({"RegExpRedundantEscape", "RegExpUnnecessaryNonCapturingGroup"})
public class URLTextExtractor {
    private static final Pattern HTML_URL_REGEX = Pattern.compile("<a href=\"(.+?)\">");

    private static final String MARKDOWN_BOLD_OR_ITALICS = "(?:\\*{0,2})";

    private static final Pattern MARKDOWN_URL_REGEX = Pattern.compile(
        MARKDOWN_BOLD_OR_ITALICS
            + "\\[(.+)\\]\\(.+\\)"
            + MARKDOWN_BOLD_OR_ITALICS
    );

    private static final Pattern STANDALONE_URL_REGEX = Pattern.compile(
        "^"
            + MARKDOWN_BOLD_OR_ITALICS
            + "(?:((?:http:\\/\\/)|(?:https:\\/\\/)|(?:www\\.)).+)"
            + MARKDOWN_BOLD_OR_ITALICS
            + "$"
    );

    private static final String REMOVED = "[removed]";
    private static final String DELETED = "[deleted]";

    private URLTextExtractor() {
    }

    private static Collection<@NotNull String> removeInvalidURLS(@NotNull Collection<String> urls) {
        UrlValidator validator = new UrlValidator();
        return urls.stream().filter(validator::isValid).toList();
    }


    /**
     * Get the URLs contained inside a Reddit post.
     *
     * @param data JSON object: must have <code>is_self</code> bool attribute, <code>id</code> string,
     *             and either <code>url</code> string or <code>selftext</code> string.
     * @return Array of URLs, guaranteed to be valid according to {@link UrlValidator#isValid(String)}
     * @throws BadRequestStatusException if HTTP status is non-<code>200</code>
     * @see URLTextExtractor#extractURLsFromRedditPost(JSONObject, String, boolean)
     */
    public static String @NotNull [] extractURLsFromRedditPost(@NotNull JSONObject data, @NotNull String subredditName) throws BadRequestStatusException {
        return extractURLsFromRedditPost(data, subredditName, false);
    }

    /**
     * Get the URLs contained inside a Reddit post.
     *
     * @param data                    JSON object: must have <code>is_self</code> bool attribute, <code>id</code> string,
     *                                and either <code>url</code> string or <code>selftext</code> string.
     * @param alwaysGetHTMLFromReddit if <code>true</code> (and post fits basic filtering requirements),
     *                                queries reddit.com HTML
     *                                (see {@link RedditClient#getPostHTML(String, String)}) to get URLs.
     *                                Otherwise, uses the <code>selftext</code> from PushShift with the HTTP query
     *                                as a last resort.
     * @return Array of URLs, guaranteed to be valid according to {@link UrlValidator#isValid(String)}
     * @throws BadRequestStatusException if HTTP status is non-<code>200</code>
     * @see URLTextExtractor#extractURLsFromRedditPost(JSONObject, String, boolean)
     */
    public static String @NotNull [] extractURLsFromRedditPost(@NotNull JSONObject data, @NotNull String subredditName, boolean alwaysGetHTMLFromReddit) throws BadRequestStatusException {
        Set<@NotNull String> unfiltered = extractURLsFromRedditPostNoFilter(data, subredditName, alwaysGetHTMLFromReddit);
        return removeInvalidURLS(unfiltered).toArray(new String[0]);
    }

    private static boolean selftextIsEmpty(@NotNull String selftext) {
        return selftext.equals(REMOVED) || selftext.equals(DELETED) || selftext.isEmpty();
    }

    private static @NotNull Collection<String> extractBase64Urls(@NotNull String selftext) {
        Base64.Decoder decoder = Base64.getDecoder();
        List<String> out = new ArrayList<>();
        for (String line : selftext.split("\n")) {
            try {
                out.add(new String(decoder.decode(line)));
            } catch (IllegalArgumentException e) {
                // no-op
            }
        }
        return out;
    }

    private static Set<@NotNull String> extractURLsFromRedditPostNoFilter(
        @NotNull JSONObject data,
        @NotNull String subredditName,
        boolean alwaysGetHTMLFromReddit
    ) throws BadRequestStatusException {
        if (data.getBoolean("is_self")) //noinspection DanglingJavadoc
        {
            /**
             See {@link RedditClient#getPostHTML(String, String)} apiNote for why we do this
             */
            final String selftext;

            selftext = data.getString("selftext").replace("\\_", "_");

            Set<String> out = new HashSet<>();
            if (selftextIsEmpty(selftext)) {
                return Set.of();
            }

            out.addAll(extractBase64Urls(selftext)); // some URLs are encoded in base 64
            out.addAll(selftext.lines().map(
                    s -> s.replaceAll("\\s", ""))
                .toList()); // some URLs are separated by spaces

            {
                Matcher matcher = MARKDOWN_URL_REGEX.matcher(selftext);
                while (matcher.find()) {
                    out.add(matcher.group(1));
                }
            }


            out.addAll(
                Arrays.stream(selftext.split("\s+"))
                    .filter(STANDALONE_URL_REGEX.asMatchPredicate())
                    .toList()
            );

            if (out.isEmpty() || alwaysGetHTMLFromReddit) {
                // Otherwise, if PushShift data isn't enough, query the html via Reddit.
                // One issue with this is that deleted posts come up empty.
                String html = RedditClient.getPostHTML(data.getString("id"), subredditName);
                Matcher matcher = HTML_URL_REGEX.matcher(html);

                while (matcher.find()) {
                    out.add(matcher.group(1));
                }
            }
            return out;

        }
        // otherwise, the post is a link post
        return Set.of(data.getString("url"));

    }
}
