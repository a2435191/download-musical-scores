package com.github.a2435191.download_musical_scores.downloaders.implementations;

import com.github.a2435191.download_musical_scores.downloaders.AbstractFileDownloader;
import com.github.a2435191.download_musical_scores.filetree.AbstractFileNode;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: make sure https://mega.nz/cmd is installed with mega-help command test

public final class MegaDownloader extends AbstractFileDownloader {
    // FIXME: downloading a folder with multiple files hangs

    private static final Pattern DOWNLOAD_PATH_REGEX = Pattern.compile("^Download finished: (.+)$");
    private static final long PROCESS_WAIT_MILLIS = 500;
    private final @NotNull Path pathToMegaCommands;


    public MegaDownloader() {
        this.pathToMegaCommands = Path.of("mega-get");
    }

    public MegaDownloader(@NotNull Path pathToMegaCommands) {
        this.pathToMegaCommands = pathToMegaCommands;
    }

    private static @NotNull Path getDownloadPathFromOutputText(@NotNull Collection<@NotNull String> outputLines) {
        String stringPath = outputLines
            .stream()
            .map(s -> {
                Matcher m = DOWNLOAD_PATH_REGEX.matcher(s);
                if (m.matches()) {
                    return m.group(1);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                "No line of output " + String.join("\n", outputLines) + " matches regex " + DOWNLOAD_PATH_REGEX + "!")
            );
        return Path.of(stringPath);
    }

    private static void readerToLinesBuffer(@NotNull BufferedReader reader, @NotNull Collection<String> original) {
        try {
            while (reader.ready()) {
                String line = reader.readLine();
                original.add(line);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull AbstractFileNode getFileTreeRoot(@NotNull String url) {
        return new AbstractFileNode() {
            @Override
            public @NotNull Path saveToDisk(@NotNull Path parentDir) throws IOException {

                final String[] command;
                if (parentDir.toString().isEmpty()) {
                    command = new String[]{
                        pathToMegaCommands.toString(), url
                    };
                } else {
                    command = new String[]{
                        pathToMegaCommands.toString(), url, parentDir.toString()
                    };
                }
                Process p = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

                Set<String> outLines = new HashSet<>();
                BufferedReader outReader = new BufferedReader(new InputStreamReader(p.getInputStream()));

                boolean isDone;
                do {
                    isDone = true;
                    try {
                        isDone = p.waitFor(PROCESS_WAIT_MILLIS, TimeUnit.MILLISECONDS);
                        readerToLinesBuffer(outReader, outLines);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } while (!isDone);

                int status = p.exitValue();

                if (status != 0) {
                    String errorMessage = String.join("\n", outLines);
                    throw new IOException(String.format("Non-zero status code %d while running %s:\n%s",
                        status, String.join(" ", command), errorMessage)
                    );
                }

                return getDownloadPathFromOutputText(outLines);
            }
        };
    }
}
