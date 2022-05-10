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
                .addMegaDownloader();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MusicalScoresDownloader(@NotNull AbstractFileDownloader.DownloaderManager manager) {
        this.manager = manager;
    }

    public void download(@NotNull String url, @NotNull Path parentDir) throws IOException {
        System.out.println("here with " + url + " and " + parentDir);
        final AbstractFileDownloader downloader = this.manager.getInstanceFromUrl(URI.create(url));

        System.out.println("got url");
        if (downloader == null) {
            throw new RuntimeException("no downloader found for url " + url + "!");
        }


        System.out.println("before root");
        System.out.println(downloader.hashCode());

        AbstractFileNode root = downloader.getFileTreeRoot(url);
        System.out.println("getting file tree root");
        Stack<NodeAndPath> stack = new Stack<>();
        stack.add(new NodeAndPath(root, parentDir));

        while (!stack.isEmpty()) {
            System.out.println(stack);
            NodeAndPath nodeAndDownloadDir = stack.pop();


            if (!nodeAndDownloadDir.downloadDir.toFile().exists()) {
                Files.createDirectory(nodeAndDownloadDir.downloadDir);
            }
            Path downloadPath = nodeAndDownloadDir.node.saveToDisk(nodeAndDownloadDir.downloadDir);
            for (AbstractFileNode child : nodeAndDownloadDir.node.getChildren()) {
                stack.add(new NodeAndPath(child, nodeAndDownloadDir.downloadDir));
            }
        }
    }

    // downloadDir is parent dir for download into
    private record NodeAndPath(@NotNull AbstractFileNode node,
                               @NotNull Path downloadDir) {
    }


}