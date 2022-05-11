package com.github.a2435191.download_musical_scores.downloaders;

import com.github.a2435191.download_musical_scores.downloaders.implementations.*;
import com.github.a2435191.download_musical_scores.filetree.AbstractFileNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public abstract class AbstractFileDownloader {


    public abstract @NotNull AbstractFileNode getFileTreeRoot(@NotNull String url) throws IOException;

    @ThreadSafe
    public static class DownloaderManager {
        public final Map<BiPredicate<@NotNull URI, @NotNull String>, @NotNull Supplier<@NotNull AbstractFileDownloader>> downloaders
            = new ConcurrentHashMap<>();


        public void put(@NotNull Supplier<@NotNull AbstractFileDownloader> downloader, @NotNull String domainName) {
            downloaders.put((uri, host) -> host.equals(domainName), downloader);
        }

        public void put(@NotNull Supplier<@NotNull AbstractFileDownloader> downloader, @NotNull String... domainNames) {
            HashSet<String> domainNamesSet = new HashSet<>(List.of(domainNames));
            downloaders.put((uri, host) -> domainNamesSet.contains(host), downloader);
        }


        @Contract(value = "_ -> this", mutates = "this")
        public @NotNull DownloaderManager addGoogleDriveDownloader(int timeoutSeconds) throws GeneralSecurityException, IOException {
            this.put(() -> {
                try {
                    return new GoogleDriveDownloader(timeoutSeconds);
                } catch (GeneralSecurityException | IOException e) {
                    throw new RuntimeException(e);
                }
            }, "drive.google.com");
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public @NotNull DownloaderManager addSendspaceDownloader(int timeoutSeconds) {
            this.put(() -> new SendspaceDownloader(timeoutSeconds), "sendspace.com");
            return this;
        }

        @Contract(value = "-> this", mutates = "this")
        public @NotNull DownloaderManager addMegaDownloader() {
            this.put(MegaDownloader::new, "mega.nz", "mega.co.nz");
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public @NotNull DownloaderManager addWeTransferDownloader(int timeoutSeconds) {
            this.put(() -> new WeTransferDownloader(timeoutSeconds), "we.tl", "wetransfer.com");
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public @NotNull DownloaderManager addStackStorageDownloader(int timeoutSeconds) {
            this.downloaders.put(
                (uri, host) -> host.contains("stackstorage.com"),
                () -> new StackStorageDownloader(60)
            );
            return this;
        }

        @Contract(pure = true)
        public @Nullable AbstractFileDownloader getInstanceFromUrl(@NotNull URI url) {
            String host = url.getHost().toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) {
                host = host.substring("www.".length());
            }
            for (var pair : this.downloaders.entrySet()) {
                BiPredicate<@NotNull URI, @NotNull String> predicate = pair.getKey();
                if (predicate.test(url, host)) {
                    return pair.getValue().get();
                }
            }

            return null;
        }

    }


}
