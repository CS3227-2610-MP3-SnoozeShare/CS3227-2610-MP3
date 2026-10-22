package com.snoozeshare.ai;

/** Which configured SoCLaaS model a feature runs on; the gateway resolves it to a model name. */
public enum ModelTier {
    /** Small and cheap; the {@code model-fast} setting. */
    FAST,
    /** The default; the {@code model-standard} setting. */
    STANDARD,
    /** Largest and slowest; the {@code model-strong} setting. */
    STRONG
}
