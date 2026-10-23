package com.snoozeshare.service.assistant;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Model reply contract; missing enough must fail validation rather than default to false. */
public record ClarifyOutput(
    @NotNull Boolean enough,
    @NotNull @Size(max = 3) List<@NotNull @Size(min = 1, max = 300) String> questions,
    @NotNull @Size(min = 1, max = 500) String summary,
    @NotNull @Size(max = 6) List<@NotNull @Size(min = 1, max = 150) String> timeline,
    @NotNull @Size(max = 6) List<@NotNull @Size(min = 1, max = 200) String> claims,
    @NotNull @Size(max = 4) List<@NotNull @Size(min = 1, max = 150) String> evidence,
    @NotNull @Size(max = 5) List<@NotNull @Size(min = 1, max = 150) String> missing,
    @NotNull @Size(max = 4) List<@NotNull @Size(min = 1, max = 200) String> hostPoints) {

    /** Degraded fallback; never applied as successful model output. */
    public static final ClarifyOutput NONE = new ClarifyOutput(false, List.of(), "No draft.", List.of(),
        List.of(), List.of(), List.of(), List.of());
}
