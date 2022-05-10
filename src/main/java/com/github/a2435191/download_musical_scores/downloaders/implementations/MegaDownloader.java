package com.github.a2435191.download_musical_scores.downloaders.implementations;

import com.github.a2435191.download_musical_scores.downloaders.AbstractFileDownloader;
import com.github.a2435191.download_musical_scores.filetree.AbstractFileNode;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// TODO: make sure https://mega.nz/cmd is installed with mega-help command test

public final class MegaDownloader extends AbstractFileDownloader {

    private static final Pattern DOWNLOAD_PATH_REGEX = Pattern.compile("^Download finished: (.+)$");
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
            .filter(DOWNLOAD_PATH_REGEX.asMatchPredicate())
            .map(s -> {
                Matcher m = DOWNLOAD_PATH_REGEX.matcher(s);
                m.matches();
                return m.group(1);
            })
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                "No line of output " + String.join("\n", outputLines) + " matches regex " + DOWNLOAD_PATH_REGEX + "!")
            );
        return Path.of(stringPath);
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
                Process p = Runtime.getRuntime().exec(command);

                final int status;
                try {
                    status = p.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("thread interrupted!", e);
                }

                if (status != 0) {
                    String errorMessage = p.errorReader().lines().collect(Collectors.joining("\n"));
                    throw new IOException(String.format("Non-zero status code %d while running %s:\n%s",
                        status, String.join(" ", command), errorMessage)
                    );
                }

                return getDownloadPathFromOutputText(p.inputReader().lines().toList());
            }
        };
    }
}
