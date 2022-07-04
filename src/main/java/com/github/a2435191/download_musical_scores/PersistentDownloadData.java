package com.github.a2435191.download_musical_scores;

import com.opencsv.CSVReader;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Record representing a row of a download information file.
 *
 * @param redditID          6-digit, base-36 unique id representing a Reddit post
 * @param saveLocation      Path this link was downloaded to
 * @param linkNumber        The (zero-based) index of the link in the post
 * @param url               URL of the link (not the Reddit post)
 * @param downloadTimestamp CST timestamp representing the download time of the link.
 *                          Format: <a href="https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html#ISO_LOCAL_DATE_TIME">ISO-8601</a>.
 * @param overwrite         Whether subsequent downloads should overwrite this file.
 *                          <code>true</code> or <code>false</code>.
 */
public record PersistentDownloadData(
    @NotNull String redditID,
    @NotNull Path saveLocation,
    int linkNumber,
    @NotNull URI url,
    @NotNull LocalDateTime downloadTimestamp,
    boolean overwrite) {

    private static final @NotNull DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Create a new instance from String args.
     *
     * @param args String arguments to be converted. Must have length 6.
     * @return A new instance.
     */
    @Contract("_ -> new")
    public static @NotNull PersistentDownloadData create(@NotNull String @NotNull ... args) {
        if (args.length != 6) {
            throw new IllegalArgumentException("args must have length 6");
        }


        return new PersistentDownloadData(
            args[0],
            Path.of(args[1]),
            Integer.parseInt(args[2]),
            URI.create(args[3]),
            LocalDateTime.from(FORMATTER.parse(args[4])),
            Boolean.parseBoolean(args[5])
        );
    }


    /**
     * Create a new instance from a CSV file
     *
     * @param csvFile File to be read. The header row is ignored.
     * @return A new instance.
     * @throws IOException if the file can't be opened, read, etc.
     *                     See {@link FileReader#FileReader(File)}, {@link CSVReader#readNext()} for details.
     */
    public static @NotNull Map<Map.Entry<@NotNull String, @NotNull Integer>, @NotNull PersistentDownloadData> fromCSV(
        @NotNull File csvFile) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            reader.readNext(); // discard header
            final HashMap<Map.Entry<String, Integer>, PersistentDownloadData> out = new HashMap<>();
            reader.forEach(args -> {
                PersistentDownloadData data = PersistentDownloadData.create(args);
                Map.Entry<String, Integer> key = Map.entry(data.redditID, data.linkNumber);
                if (out.containsKey(key)) {
                    throw new RuntimeException(key + " has a duplicate");
                } else {
                    out.put(key, data);
                }
            });
            return out;
        }
    }

    /**
     * Convert this into a row suitable for a CSV.
     * @return An array of 6 strings. See
     * {@link PersistentDownloadData#PersistentDownloadData(String, Path, int, URI, LocalDateTime, boolean)}}
     * for what each element means.
     *
     * It is guaranteed that <code>create(args).toCsvRow().equals(args)</code>.
     */
    @Contract(pure = true)
    public @NotNull String @NotNull [] toCsvRow() {
        return new String[]{
            redditID,
            saveLocation.toString(),
            "" + linkNumber,
            url.toString(),
            FORMATTER.format(downloadTimestamp),
            "" + overwrite
        };
    }

}


