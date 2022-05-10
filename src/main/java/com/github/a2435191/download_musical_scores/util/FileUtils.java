package com.github.a2435191.download_musical_scores.util;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Stack;

public final class FileUtils {
    private FileUtils() {

    }

    public static void delete(@NotNull File folder) {
        Stack<@NotNull File> stack = new Stack<>();
        stack.add(folder);

        while (!stack.isEmpty()) {
            File next = stack.peek();
            File[] children = next.listFiles();

            if (next.isFile() || (children != null && children.length == 0)) { // file or empty folder
                try {
                    Files.deleteIfExists(next.toPath());
                } catch (IOException e) {
                    throw new RuntimeException("this should never, ever happen!");
                }
                stack.pop();
            } else if (children != null) { // non-empty folder
                stack.addAll(Arrays.asList(children));
            }
        }

    }
}
