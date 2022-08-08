package com.github.a2435191.download_musical_scores;


import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class Main {

    public static final String DOWNLOAD_CSV_PATH = "downloads_test.csv";

    @SuppressWarnings("RedundantThrows")
    public static void main(String[] args) throws Throwable {
        MusicalScoresDownloader downloader = new MusicalScoresDownloader(
            "MusicalScores", Path.of("downloads"), new File("downloads.csv"));


        CompletableFuture<Void> downloadFuture = CompletableFuture.runAsync(
            () -> downloader.downloadAll(20, 0, -1).joinAll()
        );

//        CompletableFuture<Void> savePermanentData = CompletableFuture.runAsync(
//            () -> {
//                try (
//                    FileWriter fileWriterToErase = new FileWriter(DOWNLOAD_CSV_PATH, false);
//                    FileWriter fileWriter = new FileWriter(DOWNLOAD_CSV_PATH, true);
//                    CSVWriter writer = new CSVWriter(fileWriter)
//                ) {
//                    fileWriterToErase.write("");
//                    fileWriterToErase.flush();
//
//                    writer.writeNext(PersistentDownloadData.FIELDS);
//                    writer.flush();
//                    while (!downloadFuture.isDone()) {
//                        MusicalScoresDownloader.sleep(500);
//                        @Nullable PersistentDownloadData data = downloader.outData.poll();
//                        if (data != null) {
//                            writer.writeNext(data.toCsvRow());
//                            writer.flush();
//                        }
//                    }
//
//
//                } catch (IOException ex) {
//                    throw new RuntimeException(ex);
//                }
//            }
//        );
//        savePermanentData.join();
        downloadFuture.join();
        System.out.println(downloader.outData);


    }
}
