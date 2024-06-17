package net.rk4z.mcef.progress

import net.rk4z.mcef.MCEF

class MCEFProgressTracker {

    var task: String? = null
        private set
    var progress: Float = 0f
        private set
    var isDone: Boolean = false
        private set

    private var loggedPercent: Float = 0f

    fun setTask(name: String) {
        task = name
        progress = 0f
        MCEF.logger.info("[$task] Started task")
    }

    fun setProgress(percent: Float) {
        progress = percent.coerceIn(0f, 1f)

        if ((progress * 100).toInt() != (loggedPercent * 100).toInt()) {
            MCEF.logger.info("[$task] Progress ${(progress * 100).toInt()}%")
            loggedPercent = progress
        }
    }

    fun done() {
        isDone = true
        MCEF.logger.info("[$task] Finished task")
    }
}

