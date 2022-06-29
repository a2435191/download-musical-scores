package com.github.a2435191.download_musical_scores;

import com.github.a2435191.download_musical_scores.downloaders.AbstractFileDownloader;
import com.github.a2435191.download_musical_scores.filetree.AbstractFileNode;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Stack;

@ThreadSafe
public class MusicalScoresDownloader {
    private final AbstractFileDownloader.DownloaderManager manager;

    public MusicalScoresDownloader() {
        try {
            this.manager = new AbstractFileDownloader.DownloaderManager()
                .addWeTransferDownloader(60)
                .addStackStorageDownloader(60)
                .addGoogleDriveDownloader(60)
                .addMegaDownloader()
                .addDropboxDownloader();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MusicalScoresDownloader(@NotNull AbstractFileDownloader.DownloaderManager manager) {
        this.manager = manager;
    }

    public void download(@NotNull String url, @NotNull Path parentDir) throws IOException {
        final AbstractFileDownloader downloader = this.manager.getInstanceFromUrl(URI.create(url));

        if (downloader == null) {
            throw new RuntimeException("no downloader found for url " + url + "!");
        }


        AbstractFileNode root = downloader.getFileTreeRoot(url);
        Stack<NodeAndPath> stack = new Stack<>();
        stack.add(new NodeAndPath(root, parentDir));

        while (!stack.isEmpty()) {
            NodeAndPath nodeAndDownloadDir = stack.pop();


            if (!nodeAndDownloadDir.downloadDir.toFile().exists()) {
                Files.createDirectory(nodeAndDownloadDir.downloadDir);
            }
            Path downloadPath = nodeAndDownloadDir.node.saveToDisk(nodeAndDownloadDir.downloadDir);
            for (AbstractFileNode child : nodeAndDownloadDir.node.getChildren()) {
                stack.add(new NodeAndPath(child, downloadPath));
            }
        }
    }

    // downloadDir is parent dir for download into
    private record NodeAndPath(@NotNull AbstractFileNode node,
                               @NotNull Path downloadDir) {
    }


}
