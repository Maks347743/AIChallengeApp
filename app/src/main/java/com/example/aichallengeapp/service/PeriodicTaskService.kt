package com.example.aichallengeapp.service

import android.app.Notification
import android.app.Service
import android.os.Build
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.aichallengeapp.R
import com.example.aichallengeapp.core.periodictask.domain.model.PeriodicTask
import com.example.aichallengeapp.core.periodictask.domain.model.PeriodicTaskConstants
import com.example.aichallengeapp.core.periodictask.domain.model.PeriodicTaskMessageBus
import com.example.aichallengeapp.core.periodictask.domain.repository.PeriodicTaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class PeriodicTaskService : Service() {

    private val periodicTaskRepository: PeriodicTaskRepository by inject()
    private val periodicTaskExecutor: PeriodicTaskExecutor by inject()
    private val periodicTaskMessageBus: PeriodicTaskMessageBus by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val taskJobs = ConcurrentHashMap<String, Job>()
    private val taskIntervals = ConcurrentHashMap<String, Int>()

    override fun onCreate() {
        super.onCreate()
        startForeground(PeriodicTaskConstants.NOTIFICATION_ID, buildNotification(0))
        observeActiveTasks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        taskJobs.clear()
        taskIntervals.clear()
        super.onDestroy()
    }

    private fun observeActiveTasks() {
        serviceScope.launch {
            periodicTaskRepository.getAllActive().collect { tasks ->
                val activeIds = tasks.map { it.id }.toSet()

                // Cancel jobs for tasks that are no longer active
                taskJobs.keys.filter { it !in activeIds }.forEach { id ->
                    taskJobs.remove(id)?.cancel()
                    taskIntervals.remove(id)
                    Timber.tag(TAG).d("Cancelled job for task $id")
                }

                // Start or restart jobs
                tasks.forEach { task ->
                    val existingInterval = taskIntervals[task.id]
                    if (existingInterval == null) {
                        // New task
                        taskJobs[task.id] = launchTaskJob(task)
                        taskIntervals[task.id] = task.intervalMinutes
                        Timber.tag(TAG).d("Started job for task ${task.id} (${task.toolName}, every ${task.intervalMinutes}min)")
                    } else if (existingInterval != task.intervalMinutes) {
                        // Interval changed — restart job
                        taskJobs.remove(task.id)?.cancel()
                        taskJobs[task.id] = launchTaskJob(task)
                        taskIntervals[task.id] = task.intervalMinutes
                        Timber.tag(TAG).d("Restarted job for task ${task.id} (interval changed: ${existingInterval}min → ${task.intervalMinutes}min)")
                    }
                }

                updateNotification(tasks.size)

                if (tasks.isEmpty()) {
                    Timber.tag(TAG).d("No active tasks, stopping service")
                    stopSelf()
                }
            }
        }
    }

    private fun launchTaskJob(task: PeriodicTask): Job {
        return serviceScope.launch {
            val intervalMs = task.intervalMinutes * 60_000L

            // First execution: wait full interval (for new tasks) or remaining time (for resumed tasks)
            val now = System.currentTimeMillis()
            val lastExec = task.lastExecutedAt
            val initialDelay = if (lastExec == null) {
                intervalMs
            } else {
                (intervalMs - (now - lastExec)).coerceAtLeast(0L)
            }
            delay(initialDelay)

            while (true) {
                try {
                    val message = periodicTaskExecutor.execute(task)
                    if (message != null) {
                        periodicTaskMessageBus.emit(message)
                        Timber.tag(TAG).d("Task ${task.id} executed, result posted to chat ${task.chatId}")
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Task ${task.id} failed, will retry after interval")
                }

                delay(intervalMs)
            }
        }
    }

    private fun buildNotification(taskCount: Int): Notification {
        val text = if (taskCount == 0) {
            "Starting periodic tasks..."
        } else {
            "Running $taskCount periodic task(s)"
        }
        return NotificationCompat.Builder(this, PeriodicTaskConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AI Challenge App")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(taskCount: Int) {
        val notificationManager = getSystemService(android.app.NotificationManager::class.java)
        notificationManager.notify(PeriodicTaskConstants.NOTIFICATION_ID, buildNotification(taskCount))
    }

    companion object {
        private const val TAG = "PeriodicTaskService"

        fun start(context: Context) {
            val intent = Intent(context, PeriodicTaskService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PeriodicTaskService::class.java)
            context.stopService(intent)
        }
    }
}
