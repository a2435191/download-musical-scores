package com.github.a2435191.download_musical_scores;


import com.github.a2435191.download_musical_scores.downloaders.implementations.GoogleDriveDownloader;

import java.io.File;
import java.nio.file.Path;


public final class Main {
    @SuppressWarnings("RedundantThrows")
    public static void main(String[] args) throws Throwable {
        MusicalScoresDownloader downloader = new MusicalScoresDownloader(
            "MusicalScores", Path.of("downloads"), new File("downloads.csv"));
        downloader.downloadAll(15, true);
    }
}
