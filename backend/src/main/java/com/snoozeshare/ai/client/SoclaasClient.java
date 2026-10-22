package com.snoozeshare.ai.client;

import static com.snoozeshare.ai.LlmErrorCode.AUTH;
import static com.snoozeshare.ai.LlmErrorCode.BAD_REQUEST;
import static com.snoozeshare.ai.LlmErrorCode.BUDGET_EXHAUSTED;
import static com.snoozeshare.ai.LlmErrorCode.FORBIDDEN;
import static com.snoozeshare.ai.LlmErrorCode.INVALID_OUTPUT;
import static com.snoozeshare.ai.LlmErrorCode.NETWORK;
import static com.snoozeshare.ai.LlmErrorCode.RATE_LIMITED;
import static com.snoozeshare.ai.LlmErrorCode.SERVER;
import static com.snoozeshare.ai.LlmErrorCode.TIMEOUT;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.snoozeshare.ai.config.LlmProperties;

/**
 * One HTTP attempt against the SoCLaaS OpenAI-compatible API, with the response classified into an
 * {@link AttemptResult}. The client holds one shared {@link HttpClient} (HTTP/1.1, redirects never followed),
 * never retries, never logs, and keeps the key, the upstream error body and the prompt out of every result and
 * exception message.
 */
public final class SoclaasClient {

    /** Largest 2xx body read; more is INVALID_OUTPUT. */
    static final int MAX_BODY_BYTES = 262_144;
    /** Bytes of a 429 body read for classification. */
    static final int MAX_ERROR_BYTES = 4_096;
    private static final long GRACE_MS = 250;
    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ai-client-watchdog");
        thread.setDaemon(true);
        return thread;
    });

    private static final int BUDGET_TEXT_CHARS = 200;
    private static final Pattern BUDGET = Pattern.compile("\\b(quota|budget|credits?)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RATE_LIMIT = Pattern.compile("\\brate[ -]?limit", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHOLE_SECONDS = Pattern.compile("\\d{1,9}");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private final LlmProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(CONNECT_TIMEOUT)
        .build();

    /**
     * Creates the client.
     *
     * @param props the gateway settings; supplies the base URL, key and the optional request fields
     */
    public SoclaasClient(LlmProperties props) {
        this.props = props;
    }

    /**
     * One chat message sent to the model.
     *
     * @param role {@code system}, {@code user} or {@code assistant}
     * @param content the message text
     */
    public record Message(String role, String content) {
        @Override
        public String toString() {
            return "Message[role=" + role + "]";
        }
    }

    /**
     * The request of one attempt.
     *
     * @param model the resolved model name
     * @param messages the assembled prompt
     * @param maxTokens the {@code max_tokens} value
     * @param temperature the {@code temperature} value
     * @param json true for a JSON feature, which sends {@code response_format} when the setting is on
     */
    public record Request(String model, List<Message> messages, int maxTokens, double temperature, boolean json) {
        @Override
        public String toString() {
            return "Request[model=" + model + ", messages=" + messages.size() + "]";
        }
    }

    /**
     * The result of {@link #listModels(Duration)}.
     *
     * @param status the HTTP status, 0 when no response was received
     * @param ids the model ids of {@code data[].id}, or null when the call failed
     */
    public record ModelsResult(int status, Set<String> ids) {
        /**
         * Whether the model list was read.
         *
         * @return true for a 200 reply with a parsable list
         */
        public boolean ok() {
            return ids != null;
        }
    }

    /**
     * Sends one chat completion request and classifies the response. Never throws for an expected failure.
     *
     * @param request the request
     * @param timeout the longest the attempt may take before its response arrives
     * @return a {@link AttemptResult.Success} or a {@link AttemptResult.Failure}
     */
    public AttemptResult attempt(Request request, Duration timeout) {
        byte[] body;
        try {
            body = mapper.writeValueAsBytes(buildBody(request));
        } catch (IOException e) {
            return new AttemptResult.Failure(BAD_REQUEST, null, false, 0);
        }
        HttpRequest.Builder builder = baseRequest("/chat/completions", timeout)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        try {
            return classify(fetch(builder.build(), timeout));
        } catch (HttpTimeoutException e) {
            return new AttemptResult.Failure(TIMEOUT, null, false, 0);
        } catch (IOException e) {
            return new AttemptResult.Failure(NETWORK, null, false, 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new AttemptResult.Failure(TIMEOUT, null, false, 0);
        } catch (RuntimeException e) {
            return new AttemptResult.Failure(NETWORK, null, false, 0);
        }
    }

    /**
     * Reads {@code GET {base-url}/models}.
     *
     * @param timeout the longest the call may take, response body included
     * @return the model ids, or a result without ids when the call failed
     */
    public ModelsResult listModels(Duration timeout) {
        HttpRequest request = baseRequest("/models", timeout).GET().build();
        try {
            Fetched fetched = fetch(request, timeout);
            if (fetched.status() != 200 || fetched.body().length > MAX_BODY_BYTES) {
                return new ModelsResult(fetched.status(), null);
            }
            JsonNode data = mapper.readTree(fetched.body()).path("data");
            if (!data.isArray()) {
                return new ModelsResult(fetched.status(), null);
            }
            Set<String> ids = new LinkedHashSet<>();
            for (JsonNode item : data) {
                if (item.path("id").isTextual()) {
                    ids.add(item.path("id").asText());
                }
            }
            return new ModelsResult(fetched.status(), ids);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ModelsResult(0, null);
        } catch (IOException | RuntimeException e) {
            return new ModelsResult(0, null);
        }
    }

    /** A complete response: status, headers and a body of at most {@code MAX_BODY_BYTES + 1} bytes. */
    private record Fetched(int status, HttpHeaders headers, byte[] body) { }

    /**
     * Sends the request and reads the whole response within {@code timeout}, so a server that stalls before or
     * after the headers cannot hold the calling thread. A watchdog closes the body stream when time runs out; the
     * body is read up to {@code MAX_BODY_BYTES + 1} bytes.
     */
    private Fetched fetch(HttpRequest request, Duration timeout) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos() + TimeUnit.MILLISECONDS.toNanos(GRACE_MS);
        CompletableFuture<HttpResponse<InputStream>> future =
            http.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
        HttpResponse<InputStream> response;
        try {
            response = future.get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new HttpTimeoutException("response headers not received in time");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IOException io) {
                throw io;
            }
            throw new IOException(e.getCause().getClass().getName());
        } catch (InterruptedException e) {
            future.cancel(true);
            throw e;
        }
        AtomicBoolean expired = new AtomicBoolean();
        try (InputStream in = response.body()) {
            ScheduledFuture<?> watchdog = WATCHDOG.schedule(() -> {
                expired.set(true);
                try {
                    in.close();
                } catch (IOException e) {
                    // the read below fails or ends, which is all the watchdog needs
                }
            }, Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            byte[] body;
            try {
                body = in.readNBytes(MAX_BODY_BYTES + 1);
            } catch (IOException e) {
                if (expired.get()) {
                    throw new HttpTimeoutException("response body not received in time");
                }
                throw e;
            } finally {
                watchdog.cancel(false);
            }
            if (expired.get()) {
                throw new HttpTimeoutException("response body not received in time");
            }
            return new Fetched(response.statusCode(), response.headers(), body);
        }
    }

    private HttpRequest.Builder baseRequest(String path, Duration timeout) {
        return HttpRequest.newBuilder(URI.create(props.baseUrl() + path))
            .timeout(timeout)
            .header("Authorization", "Bearer " + props.apiKey())
            .header("Accept", "application/json");
    }

    private ObjectNode buildBody(Request request) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", request.model());
        ArrayNode messages = root.putArray("messages");
        for (Message m : request.messages()) {
            messages.addObject().put("role", m.role()).put("content", m.content());
        }
        root.put("max_tokens", request.maxTokens());
        root.put("temperature", request.temperature());
        if (request.json() && props.responseFormatJson()) {
            root.putObject("response_format").put("type", "json_object");
        }
        if (!props.reasoningEffort().isEmpty()) {
            root.put("reasoning_effort", props.reasoningEffort());
        }
        return root;
    }

    private AttemptResult classify(Fetched response) {
        int status = response.status();
        if (status == 200) {
            return parseSuccess(response.body());
        }
        if (status >= 200 && status < 300) {
            return new AttemptResult.Failure(INVALID_OUTPUT, null, false, status);
        }
        return switch (status) {
            case 401 -> new AttemptResult.Failure(AUTH, null, false, status);
            case 403 -> new AttemptResult.Failure(FORBIDDEN, null, false, status);
            case 429 -> classifyTooMany(response);
            default -> new AttemptResult.Failure(status >= 500 ? SERVER : BAD_REQUEST, null, false, status);
        };
    }

    private AttemptResult classifyTooMany(Fetched response) {
        Integer retryAfter = response.headers().firstValue("Retry-After").map(SoclaasClient::parseSeconds)
            .orElse(null);
        byte[] body = response.body();
        byte[] head = body.length > MAX_ERROR_BYTES ? Arrays.copyOf(body, MAX_ERROR_BYTES) : body;
        String text = errorText(head);
        String window = text.length() > BUDGET_TEXT_CHARS ? text.substring(0, BUDGET_TEXT_CHARS) : text;
        if (BUDGET.matcher(window).find() && !RATE_LIMIT.matcher(window).find()) {
            return new AttemptResult.Failure(BUDGET_EXHAUSTED, retryAfter, true, 429);
        }
        return new AttemptResult.Failure(RATE_LIMITED, retryAfter, false, 429);
    }

    /** The {@code error} string or {@code error.message} of a JSON body; empty for anything else. */
    private String errorText(byte[] body) {
        try {
            JsonNode error = mapper.readTree(body).path("error");
            if (error.isTextual()) {
                return error.asText();
            }
            if (error.isObject() && error.path("message").isTextual()) {
                return error.path("message").asText();
            }
        } catch (IOException | RuntimeException e) {
            return "";
        }
        return "";
    }

    private AttemptResult parseSuccess(byte[] bytes) {
        if (bytes.length > MAX_BODY_BYTES) {
            return new AttemptResult.Failure(INVALID_OUTPUT, null, false, 200);
        }
        JsonNode root;
        try {
            root = mapper.readTree(bytes);
        } catch (IOException e) {
            return new AttemptResult.Failure(INVALID_OUTPUT, null, false, 200);
        }
        JsonNode choice = root.path("choices").path(0);
        JsonNode content = choice.path("message").path("content");
        if (!content.isTextual() || content.asText().isBlank()) {
            return new AttemptResult.Failure(INVALID_OUTPUT, null, false, 200);
        }
        JsonNode reason = choice.path("finish_reason");
        JsonNode usage = root.path("usage");
        return new AttemptResult.Success(content.asText(), reason.isTextual() ? reason.asText() : null,
            usage.path("prompt_tokens").asInt(0), usage.path("completion_tokens").asInt(0));
    }

    private static Integer parseSeconds(String header) {
        String value = header.trim();
        return WHOLE_SECONDS.matcher(value).matches() ? Integer.valueOf(value) : null;
    }
}
