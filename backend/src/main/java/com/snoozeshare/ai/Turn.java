package com.snoozeshare.ai;

/**
 * One message of the conversation sent to the model.
 *
 * @param role who spoke
 * @param text the raw text; the gateway sanitizes it before it leaves the process
 */
public record Turn(TurnRole role, String text) { }
