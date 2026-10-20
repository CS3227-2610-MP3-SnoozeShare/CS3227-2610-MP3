package com.snoozeshare.testsupport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads a source or resource file with {@code \n} line endings, so tests that match multi-line snippets pass
 * whether the working copy was checked out with LF or CRLF (git's autocrlf on Windows).
 */
public final class SourceText {

    private SourceText() {
    }

    public static String read(String first, String... more) throws IOException {
        return Files.readString(Path.of(first, more)).replace("\r\n", "\n");
    }
}
