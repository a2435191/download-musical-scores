package com.github.a2435191.download_musical_scores.filetree;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractFileNodeStreamDownloader extends AbstractFileNode {
    @Override
    public final @NotNull Path saveToDisk(@NotNull Path parentDir) throws IOException {
        FileInfo info = this.download();
        Path fullPath = parentDir.resolve(info.name());
        if (this.isDirectory()) {
            Files.createDirectory(fullPath);
        } else {
            Files.createFile(fullPath);
            Files.write(fullPath, info.data().readAllBytes());
        }
        return fullPath;
    }

    protected abstract @NotNull FileInfo download() throws IOException;
}
