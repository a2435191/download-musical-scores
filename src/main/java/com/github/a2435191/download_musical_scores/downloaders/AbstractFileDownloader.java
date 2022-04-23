package com.github.a2435191.download_musical_scores.downloaders;

import com.github.a2435191.download_musical_scores.downloaders.implementations.GoogleDriveDownloader;
import com.github.a2435191.download_musical_scores.downloaders.implementations.MegaDownloader;
import com.github.a2435191.download_musical_scores.downloaders.implementations.SendspaceDownloader;
import com.github.a2435191.download_musical_scores.downloaders.implementations.WeTransferDownloader;
import com.github.a2435191.download_musical_scores.filetree.AbstractFileNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;

public abstract class AbstractFileDownloader {




    public abstract @NotNull AbstractFileNode getFileTreeRoot(@NotNull String url) throws IOException;

    public static class DownloaderManager {
        private final @NotNull WeTransferDownloader weTransferDownloader;
        private final @NotNull GoogleDriveDownloader googleDriveDownloader;
        private final @NotNull SendspaceDownloader sendspaceDownloader;
        private final @NotNull MegaDownloader megaDownloader;

        public DownloaderManager(
            @NotNull WeTransferDownloader weTransferDownloader,
            @NotNull GoogleDriveDownloader googleDriveDownloader,
            @NotNull SendspaceDownloader sendspaceDownloader,
            @NotNull MegaDownloader megaDownloader
            ) {
            this.weTransferDownloader = weTransferDownloader;
            this.googleDriveDownloader = googleDriveDownloader;
            this.sendspaceDownloader = sendspaceDownloader;
            this.megaDownloader = megaDownloader;
        }

        @Contract(pure = true)
        public @Nullable AbstractFileDownloader getInstanceFromUrl(@NotNull URI url) {
            String host = url.getHost();
            if (host.startsWith("www.")) {
                host = host.substring("www.".length());
            }
            return switch (host) {
                case "we.tl", "wetransfer.com" -> this.weTransferDownloader;
                case "drive.google.com" -> this.googleDriveDownloader;
                case "sendspace.com" -> this.sendspaceDownloader;
                //case "mega.nz" -> this.megaDownloader;
                default -> null;
            };
        }

    }


}
