package com.github.a2435191.download_musical_scores.util;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class FileUtils {
    private static final int ZIP_BUFSIZE = 2048;

    private FileUtils() {
    }

    public static void delete(@NotNull Path folder) throws IOException {
        Files.walkFileTree(folder, new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                throw exc;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void zipDirectory(@NotNull Path directory, @NotNull Path out) throws IOException {
        try (
            FileOutputStream outputStream = new FileOutputStream(out.toString());
            ZipOutputStream zipOut = new ZipOutputStream(outputStream)
        ) {
            Files.walkFileTree(directory, (SimpleFileVisitor) (Path file, BasicFileAttributes attrs) -> {
                try (FileInputStream input = new FileInputStream(file.toFile())) {
                    ZipEntry zipEntry = new ZipEntry(directory.relativize(file).toString());
                    zipOut.putNextEntry(zipEntry);
                    byte[] buf = new byte[ZIP_BUFSIZE];
                    int chunkLen;
                    while ((chunkLen = input.read(buf)) >= 0) {
                        zipOut.write(buf, 0, chunkLen);
                    }
                }
                return FileVisitResult.CONTINUE;
            });
        }


    }

    public static void serialize(Object obj, @NotNull File file)
        throws IOException
    {
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream out = new ObjectOutputStream(fos)) {
            out.writeObject(obj);
        }
    }

    public static Object deserialize(@NotNull File file) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream in = new ObjectInputStream(fis)) {
            return in.readObject();
        }
    }

    @FunctionalInterface
    private interface SimpleFileVisitor extends FileVisitor<Path> {
        @Override
        default FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException;

        @Override
        default FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            return FileVisitResult.CONTINUE;
        }

        @Override
        default FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }
}