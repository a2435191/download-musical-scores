package com.github.a2435191.download_musical_scores.downloaders;

import com.github.a2435191.download_musical_scores.downloaders.implementations.*;
import com.github.a2435191.download_musical_scores.filetree.AbstractFileNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

public abstract class AbstractFileDownloader {


    public abstract @NotNull AbstractFileNode getFileTreeRoot(@NotNull String url) throws IOException;

    public static class DownloaderManager {
        private final @NotNull WeTransferDownloader weTransferDownloader;
        private final @NotNull GoogleDriveDownloader googleDriveDownloader;
        private final @NotNull SendspaceDownloader sendspaceDownloader;
        private final @NotNull MegaDownloader megaDownloader;
        private final @NotNull StackStorageDownloader stackStorageDownloader;

        public DownloaderManager(
            @NotNull WeTransferDownloader weTransferDownloader,
            @NotNull GoogleDriveDownloader googleDriveDownloader,
            @NotNull SendspaceDownloader sendspaceDownloader,
            @NotNull MegaDownloader megaDownloader,
            @NotNull StackStorageDownloader stackStorageDownloader
        ) {
            this.weTransferDownloader = weTransferDownloader;
            this.googleDriveDownloader = googleDriveDownloader;
            this.sendspaceDownloader = sendspaceDownloader;
            this.megaDownloader = megaDownloader;
            this.stackStorageDownloader = stackStorageDownloader;
        }

        @Contract(pure = true)
        public @Nullable AbstractFileDownloader getInstanceFromUrl(@NotNull URI url) {
            String host = url.getHost().toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) {
                host = host.substring("www.".length());
            }
            if (host.contains("stackstorage.com")) {
                return this.stackStorageDownloader;
            }
            return switch (host) {
                //case "we.tl", "wetransfer.com" -> this.weTransferDownloader;
                case "drive.google.com" -> this.googleDriveDownloader;
                case "sendspace.com" -> this.sendspaceDownloader;
                //case "mega.nz" -> this.megaDownloader;
                default -> null;
            };
        }

    }


}
