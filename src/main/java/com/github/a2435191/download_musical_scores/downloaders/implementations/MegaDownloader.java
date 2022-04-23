package com.github.a2435191.download_musical_scores.downloaders.implementations;

import com.github.a2435191.download_musical_scores.downloaders.AbstractFileDownloader;
import com.github.a2435191.download_musical_scores.filetree.AbstractFileNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

// TODO: make sure https://mega.nz/cmd is installed with mega-help command test

public class MegaDownloader extends AbstractFileDownloader {
    public @NotNull String username;
    public @NotNull String password;
    public MegaDownloader(@NotNull String username, @NotNull String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public @NotNull AbstractFileNode getFileTreeRoot(@NotNull String url)  {
        // TODO: implement
        throw new UnsupportedOperationException("not implemented!");
    }
}
