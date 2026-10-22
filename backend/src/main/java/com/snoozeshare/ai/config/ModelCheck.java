package com.snoozeshare.ai.config;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.snoozeshare.ai.client.SoclaasClient;
import com.snoozeshare.ai.limit.LlmScheduler;
import com.snoozeshare.config.ConditionalOnDatabase;

/**
 * Once at startup, when the gateway is enabled and not in mock mode, asks SoCLaaS for its model list and logs one
 * WARN for every configured model that is missing, or one WARN with the status when the request fails. It runs on
 * its own executor and never fails or delays startup.
 */
@Component
@ConditionalOnDatabase
public class ModelCheck {

    private static final Logger LOG = LoggerFactory.getLogger(ModelCheck.class);
    private static final int PRIORITY = 2;

    private final LlmProperties props;
    private final SoclaasClient client;
    private final LlmScheduler scheduler;
    private final Clock clock;
    private final Executor executor;

    /** Creates the check with a single daemon thread of its own. */
    @Autowired
    public ModelCheck(LlmProperties props, SoclaasClient client, LlmScheduler scheduler, Clock clock) {
        this(props, client, scheduler, clock, Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ai-model-check");
            thread.setDaemon(true);
            return thread;
        }));
    }

    /** Creates the check on the given executor; tests pass a direct one. */
    public ModelCheck(LlmProperties props, SoclaasClient client, LlmScheduler scheduler, Clock clock,
                      Executor executor) {
        this.props = props;
        this.client = client;
        this.scheduler = scheduler;
        this.clock = clock;
        this.executor = executor;
    }

    /**
     * Starts the check when the application is ready.
     *
     * @param event the ready event
     */
    @EventListener
    public void onReady(ApplicationReadyEvent event) {
        if (props.enabled() && !props.mock()) {
            executor.execute(this::check);
        }
    }

    /** Runs the check on the calling thread. */
    public void check() {
        try (LlmScheduler.Permit permit = scheduler.acquire(PRIORITY, clock.instant(),
            Duration.ofMillis(props.queueWaitMs()))) {
            SoclaasClient.ModelsResult result = client.listModels(Duration.ofMillis(props.requestTimeoutMs()));
            if (!result.ok()) {
                LOG.warn("SoCLaaS model list unavailable: status={}", result.status());
                return;
            }
            List<String> missing = new ArrayList<>();
            for (String model : List.of(props.modelFast(), props.modelStandard(), props.modelStrong())) {
                if (!result.ids().contains(model)) {
                    missing.add(model);
                }
            }
            for (String model : missing) {
                LOG.warn("Configured SoCLaaS model not offered by the service: {}", model);
            }
            if (missing.isEmpty()) {
                LOG.info("SoCLaaS reachable: {} models listed, all configured models present", result.ids().size());
            }
        } catch (LlmScheduler.SchedulerRejection e) {
            LOG.warn("SoCLaaS model check skipped: reason={}", e.reason());
        } catch (RuntimeException e) {
            LOG.warn("SoCLaaS model check failed: class={}", e.getClass().getName());
        }
    }
}
