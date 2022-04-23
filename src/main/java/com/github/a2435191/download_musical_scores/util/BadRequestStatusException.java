package com.github.a2435191.download_musical_scores.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.http.HttpResponse;

/**
 * Represents an HTTP request error
 */
public class BadRequestStatusException extends IOException {
    protected final HttpResponse<?> resp;

    /**
     * Create a new instance
     *
     * @param resp Response object that will be used to instantiate <code>this</code>.
     * @apiNote <code>resp.statusCode()</code> is not checked to be non-<code>200</code>
     */
    public BadRequestStatusException(@NotNull HttpResponse<?> resp) {
        super("Status code " + resp.statusCode() + "!\n" + resp.body());
        this.resp = resp;
    }


    public static void raiseOnStatus(@NotNull HttpResponse<?> resp) throws BadRequestStatusException {
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new BadRequestStatusException(resp);
        }
    }

    /**
     * Return the response object used to create <code>this</code>
     *
     * @return the response object
     */
    public @NotNull HttpResponse<?> getResponse() {
        return resp;
    }
}
