package com.floydwiz.googlemdm.data.repository

import com.floydwiz.googlemdm.data.remote.CommandDto
import com.floydwiz.googlemdm.enterprise.command.CommandExecutor
import com.floydwiz.googlemdm.enterprise.command.model.CommandOutcome
import java.util.ArrayDeque

class FakeCommandExecutor : CommandExecutor {
    val executedCommands = mutableListOf<CommandDto>()
    private val queuedOutcomes = ArrayDeque<CommandOutcome>()
    var readyForDestructiveCommand = true

    fun enqueueOutcome(outcome: CommandOutcome) {
        queuedOutcomes.add(outcome)
    }

    override suspend fun execute(command: CommandDto): CommandOutcome {
        executedCommands += command
        return queuedOutcomes.poll() ?: CommandOutcome.Completed()
    }

    override fun isReadyForDestructiveCommand(): Boolean = readyForDestructiveCommand
}
