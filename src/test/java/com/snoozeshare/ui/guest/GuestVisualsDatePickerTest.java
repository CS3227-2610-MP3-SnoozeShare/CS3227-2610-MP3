package com.snoozeshare.ui.guest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.DatePicker;

class GuestVisualsDatePickerTest {

    private static boolean toolkitAvailable;

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
            toolkitAvailable = true;
        } catch (IllegalStateException alreadyStarted) {
            toolkitAvailable = true;
        } catch (RuntimeException | UnsatisfiedLinkError unavailable) {
            toolkitAvailable = false;
        }
        if (toolkitAvailable) {
            Platform.setImplicitExit(false);
        }
    }

    private static <T> T onFxThread(java.util.concurrent.Callable<T> work) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(work.call());
            } catch (Exception exception) {
                result.completeExceptionally(exception);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }

    @Test
    void aPastDateIsClearedAndReported() throws Exception {
        assumeTrue(toolkitAvailable, "JavaFX toolkit unavailable");
        List<String> messages = new ArrayList<>();
        DatePicker picker = onFxThread(() -> {
            DatePicker created = new DatePicker();
            GuestVisuals.blockPastDates(created, messages::add);
            created.setValue(LocalDate.now().minusDays(1));
            return created;
        });

        assertNull(onFxThread(picker::getValue));
        assertEquals(List.of("Dates in the past cannot be selected."), messages);
    }

    @Test
    void todayAndFutureDatesAreKept() throws Exception {
        assumeTrue(toolkitAvailable, "JavaFX toolkit unavailable");
        List<String> messages = new ArrayList<>();
        LocalDate future = LocalDate.now().plusDays(3);
        DatePicker picker = onFxThread(() -> {
            DatePicker created = new DatePicker();
            GuestVisuals.blockPastDates(created, messages::add);
            created.setValue(LocalDate.now());
            created.setValue(future);
            return created;
        });

        assertEquals(future, onFxThread(picker::getValue));
        assertEquals(List.of(), messages);
    }
}
