package com.github.a2435191.download_musical_scores.util;

import org.jetbrains.annotations.NotNull;

public class NoSuchElementException extends RuntimeException {
    public NoSuchElementException(@NotNull String missing) {
        super(missing + " element not found!");
    }
}
