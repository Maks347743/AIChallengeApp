package com.example.aichallengeapp.service

import android.content.Context
import com.example.aichallengeapp.core.periodictask.domain.PeriodicTaskServiceController

class PeriodicTaskServiceControllerImpl(
    private val context: Context
) : PeriodicTaskServiceController {

    override fun ensureStarted() {
        PeriodicTaskService.start(context)
    }

    override fun stop() {
        PeriodicTaskService.stop(context)
    }
}
