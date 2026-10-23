package com.snoozeshare.ai.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.snoozeshare.ai.LlmErrorCode;
import com.snoozeshare.ai.config.LlmProperties;

/** N2: a server that sends the headers and then stalls cannot hold the calling thread past the timeout. */
class SoclaasClientStallTest {

    private static LlmProperties props(String baseUrl) {
        return new LlmProperties(true, false, baseUrl, "clsk_abcdefgh_fakekey", "fast-m", "std-m", "strong-m", 55, 4,
            20, 10_000, 30_000, 45_000, 2, 5, 30_000, 900_000, 2, 200, 3_000, 20, 40_000, false, "");
    }

    @Test
    void stalledBodyEndsAsTimeoutWithinTheBound() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread stall = Thread.ofVirtual().start(() -> {
                try (Socket socket = server.accept()) {
                    OutputStream out = socket.getOutputStream();
                    out.write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 5000\r\n\r\n{"
                        .getBytes());
                    out.flush();
                    Thread.sleep(10_000);
                } catch (IOException | InterruptedException e) {
                    // the client gave up, which is the expected outcome
                }
            });
            SoclaasClient client = new SoclaasClient(props("http://127.0.0.1:" + server.getLocalPort() + "/v1"));
            long start = System.nanoTime();
            AttemptResult result = client.attempt(new SoclaasClient.Request("m",
                List.of(new SoclaasClient.Message("user", "hi")), 10, 0.1, false), Duration.ofMillis(1_000));
            long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
            stall.interrupt();
            assertThat(result).isInstanceOf(AttemptResult.Failure.class);
            assertThat(((AttemptResult.Failure) result).code()).isEqualTo(LlmErrorCode.TIMEOUT);
            assertThat(elapsedMs).isLessThan(3_000);
        }
    }
}
