package com.snoozeshare.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** SD19: only {@code com.snoozeshare.ai} imports the gateway's internal packages or talks to SoCLaaS. */
class AiBoundaryTest {

    private static final Path SOURCES = Path.of("src", "main", "java");
    private static final Pattern INTERNAL_IMPORT = Pattern.compile(
        "^\\s*import\\s+(static\\s+)?com\\.snoozeshare\\.ai\\.(config|guard|limit|client|audit)\\b.*$");

    /**
     * Finds the boundary violations in one source file.
     *
     * @param relative the path below {@code src/main/java}, with forward slashes
     * @param content the file text
     * @return one message per violation
     */
    static List<String> violations(String relative, String content) {
        List<String> found = new ArrayList<>();
        boolean insideAi = relative.startsWith("com/snoozeshare/ai/");
        boolean insideClient = relative.startsWith("com/snoozeshare/ai/client/");
        for (String line : content.split("\\R")) {
            if (!insideAi && INTERNAL_IMPORT.matcher(line).matches()) {
                found.add(relative + ": imports an internal AI package: " + line.trim());
            }
            if (!insideClient && (line.contains("java.net.http") || line.contains("chat/completions"))) {
                found.add(relative + ": talks to SoCLaaS outside ai/client: " + line.trim());
            }
        }
        return found;
    }

    @Test
    void sourceTreeRespectsTheBoundary() throws IOException {
        List<String> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String relative = SOURCES.relativize(file).toString().replace('\\', '/');
                found.addAll(violations(relative, Files.readString(file)));
            }
        }
        assertThat(found).isEmpty();
    }

    @Test
    void detectsAnInternalImportFromOutside() {
        String content = "package com.snoozeshare.service;\r\nimport com.snoozeshare.ai.guard.OutputValidator;\r\n";
        assertThat(violations("com/snoozeshare/service/Bad.java", content)).hasSize(1);
        assertThat(violations("com/snoozeshare/ai/LlmGateway.java", content)).isEmpty();
    }

    @Test
    void detectsDirectHttpUseOutsideTheClient() {
        String content = "import java.net.http.HttpClient;\nString p = \"/chat/completions\";\n";
        assertThat(violations("com/snoozeshare/service/Bad.java", content)).hasSize(2);
        assertThat(violations("com/snoozeshare/ai/LlmGateway.java", content)).hasSize(2);
        assertThat(violations("com/snoozeshare/ai/client/SoclaasClient.java", content)).isEmpty();
    }

    @Test
    void publicAiTypesAreAllowed() {
        String content = "import com.snoozeshare.ai.LlmGateway;\nimport com.snoozeshare.ai.LlmResult;\n";
        assertThat(violations("com/snoozeshare/service/Ok.java", content)).isEmpty();
    }
}
