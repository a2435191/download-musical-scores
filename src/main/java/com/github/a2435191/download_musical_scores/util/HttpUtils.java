package com.github.a2435191.download_musical_scores.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public final class HttpUtils {
    public static @NotNull String urlEncode(@NotNull Map<?, ?> map, @NotNull Collection<?> list) {
        return map
            .entrySet()
            .stream()
            .map(pair -> "" + pair.getKey() + "=" + pair.getValue())
            .collect(Collectors.joining("&")) +
                   list.stream().map(Object::toString).collect(Collectors.joining("&"));
    }
}
