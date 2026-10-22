package com.snoozeshare.ai.config;

import static com.snoozeshare.ai.testsupport.StubSoclaasServer.reply;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.snoozeshare.ai.client.SoclaasClient;
import com.snoozeshare.ai.limit.LlmScheduler;
import com.snoozeshare.ai.testsupport.StubSoclaasServer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/** E38: the startup model check warns about absent models or a failing list, and never throws. */
class ModelCheckTest {

    private StubSoclaasServer stub;
    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void setUp() throws IOException {
        stub = new StubSoclaasServer();
        logger = (Logger) LoggerFactory.getLogger(ModelCheck.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
        stub.close();
    }

    private ModelCheck check(boolean enabled) {
        LlmProperties p = new LlmProperties(enabled, false, stub.baseUrl(), "clsk_abcdefgh_fakekey",
            "fast-m", "std-m", "strong-m", 55, 4, 20, 10_000, 30_000, 45_000, 2, 5, 30_000, 900_000, 2, 200, 3_000,
            20, 40_000, false, "");
        Clock clock = Clock.systemUTC();
        return new ModelCheck(p, new SoclaasClient(p), new LlmScheduler(p, clock), clock, Runnable::run);
    }

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage)
            .toList();
    }

    @Test
    void warnsForEachMissingModel() {
        stub.script(reply(200, "{\"data\":[{\"id\":\"fast-m\"},{\"id\":\"other\"}]}"));
        check(true).onReady(null);
        assertThat(warnings()).hasSize(2);
        assertThat(warnings()).anyMatch(w -> w.contains("std-m")).anyMatch(w -> w.contains("strong-m"));
        assertThat(stub.requests().get(0).method()).isEqualTo("GET");
    }

    @Test
    void noWarningWhenAllModelsArePresent() {
        stub.script(reply(200, "{\"data\":[{\"id\":\"fast-m\"},{\"id\":\"std-m\"},{\"id\":\"strong-m\"}]}"));
        check(true).onReady(null);
        assertThat(warnings()).isEmpty();
    }

    @Test
    void failingListWarnsWithTheStatusAndDoesNotThrow() {
        stub.script(reply(503, "{}"));
        check(true).onReady(null);
        assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("503"));
    }

    @Test
    void doesNothingWhenDisabled() {
        check(false).onReady(null);
        assertThat(stub.requests()).isEmpty();
    }
}
