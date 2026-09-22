package com.floydwiz.googlemdm.enterprise.command

import com.floydwiz.googlemdm.data.remote.CommandDto
import com.floydwiz.googlemdm.enterprise.command.model.CommandOutcome

/**
 * Executes a single backend-issued command. Kept as an interface so
 * EmmRepository's tests can fake it instead of exercising the real
 * DevicePolicyManager chain.
 */
interface CommandExecutor {
    suspend fun execute(command: CommandDto): CommandOutcome

    /**
     * Cheap pre-check for WIPE/REBOOT: the caller must ack the backend
     * BEFORE invoking execute() for these, since the destructive DPM call
     * tears the device down essentially immediately, leaving no chance to
     * ack afterward. Only ack an optimistic COMPLETED when this is true.
     */
    fun isReadyForDestructiveCommand(): Boolean
}
