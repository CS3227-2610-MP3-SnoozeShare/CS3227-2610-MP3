package com.snoozeshare.ai.testsupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * An in-process SoCLaaS stand-in on a loopback port. Responses are scripted in order (the last one repeats when
 * the script runs out); every request is captured. No test contacts the real service.
 */
public final class StubSoclaasServer implements AutoCloseable {

    /** A scripted response. */
    public record Reply(int status, Map<String, String> headers, byte[] body, long delayMs) { }

    /** A captured request. */
    public record Captured(String method, String path, Map<String, String> headers, String body,
        String protocol) { }

    private final HttpServer server;
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "stub-soclaas");
        t.setDaemon(true);
        return t;
    });
    private final Queue<Reply> script = new ArrayDeque<>();
    private final List<Captured> requests = new CopyOnWriteArrayList<>();
    private Reply last = reply(500, "");

    /**
     * Starts the stub.
     *
     * @throws IOException when the port cannot be bound
     */
    public StubSoclaasServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.start();
    }

    /**
     * A reply with a plain body.
     *
     * @param status HTTP status
     * @param body response body text
     * @return the reply
     */
    public static Reply reply(int status, String body) {
        return new Reply(status, Map.of(), body.getBytes(StandardCharsets.UTF_8), 0);
    }

    /**
     * A reply with one extra header.
     *
     * @param status HTTP status
     * @param header header name
     * @param value header value
     * @param body response body text
     * @return the reply
     */
    public static Reply replyWithHeader(int status, String header, String value, String body) {
        return new Reply(status, Map.of(header, value), body.getBytes(StandardCharsets.UTF_8), 0);
    }

    /**
     * A reply sent after a delay.
     *
     * @param status HTTP status
     * @param body response body text
     * @param delayMs milliseconds to wait before answering
     * @return the reply
     */
    public static Reply delayed(int status, String body, long delayMs) {
        return new Reply(status, Map.of(), body.getBytes(StandardCharsets.UTF_8), delayMs);
    }

    /**
     * A 200 chat completion.
     *
     * @param content the message content (already JSON-safe)
     * @param finishReason the finish reason
     * @return the reply
     */
    public static Reply completion(String content, String finishReason) {
        return reply(200, "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"" + content
            + "\"},\"finish_reason\":\"" + finishReason + "\"}],\"usage\":{\"prompt_tokens\":12,"
            + "\"completion_tokens\":7}}");
    }

    /**
     * Appends responses to the script.
     *
     * @param replies the replies, served in order
     */
    public synchronized void script(Reply... replies) {
        for (Reply r : replies) {
            script.add(r);
        }
    }

    /**
     * The base URL a client should use.
     *
     * @return {@code http://127.0.0.1:port/v1}
     */
    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    /**
     * The requests received so far.
     *
     * @return a copy of the captured requests
     */
    public List<Captured> requests() {
        return new ArrayList<>(requests);
    }

    private synchronized Reply next() {
        Reply r = script.poll();
        if (r != null) {
            last = r;
        }
        return last;
    }

    private void handle(HttpExchange exchange) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        exchange.getRequestHeaders().forEach((k, v) -> headers.put(k.toLowerCase(Locale.ROOT), v.get(0)));
        String body;
        try (InputStream in = exchange.getRequestBody()) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        requests.add(new Captured(exchange.getRequestMethod(), exchange.getRequestURI().getPath(), headers, body,
            exchange.getProtocol()));
        Reply reply = next();
        try {
            if (reply.delayMs() > 0) {
                Thread.sleep(reply.delayMs());
            }
            reply.headers().forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
            boolean empty = reply.body().length == 0 || reply.status() == 204 || reply.status() == 302;
            exchange.sendResponseHeaders(reply.status(), empty ? -1 : reply.body().length);
            if (!empty) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(reply.body());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // the client gave up (timeout or close); nothing to report
        } finally {
            exchange.close();
        }
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
