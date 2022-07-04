package com.github.a2435191.download_musical_scores;

import com.github.a2435191.download_musical_scores.downloaders.AbstractFileDownloader;
import com.github.a2435191.download_musical_scores.filetree.AbstractFileNode;
import com.github.a2435191.download_musical_scores.reddit.RedditPostInfo;
import com.github.a2435191.download_musical_scores.reddit.SubredditStream;
import com.github.a2435191.download_musical_scores.util.BadRequestStatusException;
import com.github.a2435191.download_musical_scores.util.FileUtils;
import com.github.a2435191.download_musical_scores.util.JobsQueue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ThreadSafe
public final class MusicalScoresDownloader {
    private static final String SUBMISSION_PREFIX = "[SUBMISSION]";
    public final @NotNull ConcurrentLinkedQueue<@NotNull PersistentDownloadData> outData
        = new ConcurrentLinkedQueue<>();
    private final AbstractFileDownloader.DownloaderManager manager;
    private final @NotNull String subredditName;
    private final @NotNull Path downloadDir;
    private final @NotNull Predicate<@Nullable PersistentDownloadData> overwritePredicate;
    private final @NotNull BiPredicate<@NotNull RedditPostInfo, @NotNull Integer> zipPredicate;
    private final @NotNull Map<Map.Entry<@NotNull String, @NotNull Integer>, @NotNull PersistentDownloadData>
        persistentDownloadDataMap;


    public MusicalScoresDownloader(@NotNull String subredditName,
                                   @NotNull Path downloadDir,
                                   @NotNull File persistentDataCSV) {

        this.manager = new AbstractFileDownloader.DownloaderManager()
            .addWeTransferDownloader(60)
            .addStackStorageDownloader(60)
            .addGoogleDriveDownloader(60)
            .addMegaDownloader()
            .addDropboxDownloader();


        try {
            this.persistentDownloadDataMap = PersistentDownloadData.fromCSV(persistentDataCSV);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.overwritePredicate = data -> data == null || data.overwrite();
        this.zipPredicate = (info, idx) -> false;
        this.subredditName = subredditName;
        this.downloadDir = downloadDir;

    }

    public MusicalScoresDownloader(@NotNull String subredditName,
                                   @NotNull Path downloadDir,
                                   AbstractFileDownloader.DownloaderManager manager,
                                   @NotNull Predicate<@Nullable PersistentDownloadData> overwritePredicate,
                                   @NotNull BiPredicate<RedditPostInfo, Integer> zipPredicate,
                                   @NotNull Map<Map.Entry<String, Integer>, PersistentDownloadData> persistentDownloadDataMap) {
        this.subredditName = subredditName;
        this.downloadDir = downloadDir;
        this.manager = manager;
        this.overwritePredicate = overwritePredicate;
        this.zipPredicate = zipPredicate;
        this.persistentDownloadDataMap = persistentDownloadDataMap;

    }

    private static @NotNull String escapeTitle(@NotNull String title) {
        String escapedTitle = title.replace("/", "|");

        if (escapedTitle.toUpperCase().startsWith(SUBMISSION_PREFIX)) {
            escapedTitle = escapedTitle
                .substring(SUBMISSION_PREFIX.length())
                .stripLeading();
        }
        if (escapedTitle.isEmpty()) {
            return SUBMISSION_PREFIX; // TODO: make sure no duplicates in file names, this is unlikely to happen
        }
        return escapedTitle;
    }

    private static void zipAndDeleteOnExceptions(Path targetPath) {
        if (!Files.exists(targetPath)) {
            return;
        }
        Path zipDownloadPath = Path.of(targetPath + ".zip");
        try {
            FileUtils.zipDirectory(targetPath, zipDownloadPath);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        } finally {
            try {
                if (targetPath.toFile().exists()) {
                    FileUtils.delete(targetPath);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static void sleep(@SuppressWarnings("SameParameterValue") long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Contract("_, _, _, _, _ -> new")
    private static @NotNull PersistentDownloadData createPersistentDataToBeSaved(
        String redditID, boolean zipped, Path saveLocation, int linkNumber, String url) {
        return new PersistentDownloadData(
            redditID,
            zipped ? Path.of(saveLocation + ".zip") : saveLocation,
            linkNumber,
            URI.create(url),
            LocalDateTime.now(),
            false
        );
    }

    public void downloadAndDeleteOnExceptions(String url, Path targetPath) {
        try {
            download(url, targetPath);
        } catch (FileAlreadyExistsException e) {
            System.out.println("Path" + targetPath + " already exists!");
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.out.println("deleting " + targetPath + " for " + url);
            if (targetPath.toFile().exists()) {
                try {
                    FileUtils.delete(targetPath);
                } catch (IOException ex) {
                    throw new RuntimeException("failed to delete " + targetPath, ex);
                }
            }
        }
    }

    public void download(@NotNull String url, @NotNull Path parentDir) throws IOException {
        final AbstractFileDownloader downloader = this.manager.getInstanceFromUrl(URI.create(url)); // threadsafe so ok

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


    public void downloadAll(int batchSize, boolean wait) {
        SubredditStream stream = new SubredditStream(subredditName);

        Stream<@NotNull RedditPostInfo> infoStream = Stream.generate(() -> {
                try {
                    return stream.getNextPostData();
                } catch (BadRequestStatusException badRequestStatusException) {
                    if (badRequestStatusException.getResponse().statusCode() == 429) { // too many requests
                        System.out.println("waiting on 429");
                        sleep(60_000);
                    }
                    badRequestStatusException.printStackTrace(System.err);
                    return null;
                }
            })
            .takeWhile($ -> !stream.isDone())
            .filter(Objects::nonNull)
            .flatMap(Stream::of)
            .limit(10);


        JobsQueue<Void> queue = new JobsQueue<>(batchSize);
        infoStream.forEach(info -> {
            final String escapedTitle = escapeTitle(info.title());
            for (int linkNumber = 0; linkNumber < info.scoreURLs().length; linkNumber++) {
                String url = info.scoreURLs()[linkNumber];
                Path targetPath = downloadDir.resolve(escapedTitle);

                @Nullable PersistentDownloadData persistentDownloadData =
                    persistentDownloadDataMap.get(Map.entry(info.id(), linkNumber));

                final boolean overwrite = overwritePredicate.test(persistentDownloadData);
                final boolean zip = zipPredicate.test(info, linkNumber);


                System.out.println("downloading " + info.title() + " at " + url);

                final int finalLinkNumber = linkNumber;
                Supplier<CompletableFuture<Void>> futureSupplier = () -> {
                    final CompletableFuture<Void> future;
                    if (overwrite && Files.exists(targetPath)) {
                        return CompletableFuture.runAsync(() -> {
                        });
                    } else {
                        future = CompletableFuture.runAsync(() ->
                                                                this.downloadAndDeleteOnExceptions(url, targetPath));
                    }

                    // can't or won't zip
                    if (!zip || (overwrite && Files.exists(Path.of(targetPath + ".zip")))) {
                        return future.thenRunAsync(() -> outData.add(createPersistentDataToBeSaved(
                            info.id(), false, targetPath, finalLinkNumber, url)));
                    }

                    // zip
                    return future.thenRunAsync(() -> {
                        zipAndDeleteOnExceptions(targetPath);
                        outData.add(createPersistentDataToBeSaved(
                            info.id(), true, targetPath, finalLinkNumber, url));
                    });
                };

                queue.add(futureSupplier, url);
            }
        });

        if (wait)
            queue.joinAll();

    }

    // downloadDir is parent dir for download into
    private record NodeAndPath(@NotNull AbstractFileNode node,
                               @NotNull Path downloadDir) {
    }


}
