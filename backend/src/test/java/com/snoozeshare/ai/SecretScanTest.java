package com.snoozeshare.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** E40: no SoCLaaS key is committed. Test inputs such as {@code clsk_ab_cd} are shorter than a real key. */
class SecretScanTest {

    private static final Pattern KEY = Pattern.compile("clsk_[A-Za-z0-9]{8}_[A-Za-z0-9]{16,}");

    private static List<String> tracked() throws IOException, InterruptedException {
        Process git = new ProcessBuilder("git", "-C", "..", "ls-files").redirectErrorStream(true).start();
        List<String> paths = new ArrayList<>();
        try (var reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(git.getInputStream(), StandardCharsets.UTF_8))) {
            reader.lines().forEach(paths::add);
        }
        git.waitFor();
        return paths;
    }

    @Test
    void noTrackedFileHoldsAKey() throws Exception {
        List<String> hits = new ArrayList<>();
        for (String path : tracked()) {
            Path file = Path.of("..", path);
            if (!Files.isRegularFile(file)) {
                continue;
            }
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length > 2_000_000) {
                continue;
            }
            if (KEY.matcher(new String(bytes, StandardCharsets.ISO_8859_1)).find()) {
                hits.add(path);
            }
        }
        assertThat(hits).isEmpty();
    }

    @Test
    void theMatcherReportsAPlantedKey() throws IOException {
        String sample = "clsk" + "_" + "AbCdEfGh" + "_" + "0123456789abcdefXYZ";
        Path temp = Files.createTempFile("planted", ".txt");
        try {
            Files.writeString(temp, "key = " + sample);
            assertThat(KEY.matcher(Files.readString(temp)).find()).isTrue();
        } finally {
            Files.delete(temp);
        }
        assertThat(KEY.matcher("clsk_ab_cd").find()).isFalse();
        assertThat(KEY.matcher("clsk_abcdefgh_fakekey").find()).isFalse();
    }
}
