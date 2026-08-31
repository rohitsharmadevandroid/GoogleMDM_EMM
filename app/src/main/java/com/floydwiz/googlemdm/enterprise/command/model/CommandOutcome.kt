package com.floydwiz.googlemdm.enterprise.command.model

sealed class CommandOutcome {
    data class Completed(val resultData: Map<String, String>? = null) : CommandOutcome()
    data class Failed(val message: String) : CommandOutcome()
}
