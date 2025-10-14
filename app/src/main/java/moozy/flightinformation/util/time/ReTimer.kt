package moozy.flightinformation.util.time

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * 合併通道版（No-Interrupt / 醒來再決策）
 *
 * 特性：
 * - restart(): 傳送新的 deadline（System.nanoTime() + interval）
 * - cancel(): 傳送 0L（未武裝）
 * - 只保留最後一次事件（Channel.CONFLATED）
 * - 單一監控 Job，永不退出、零輪詢
 *
 * 語義：
 * - 「最後一次送出者生效」
 * - 「晚到取消」在極短視窗內可能仍會觸發（近似 cancel-wins）
 */
class ReTimer(
    private val scope: CoroutineScope,
    intervalMillis: Long,
    private val onStartOrRestart: (ReTimer.() -> Unit)? = null,
    private val onTimeout: (ReTimer.() -> Unit)? = null,
) {
    private val channel = Channel<Long>(Channel.Factory.CONFLATED)
    private val intervalNanos = TimeUnit.MILLISECONDS.toNanos(intervalMillis)
    private var job: Job? = null

    init {
        startWatcher()
    }

    private fun startWatcher() {
        job = scope.launch {
            while (true) {
                var deadline = channel.receive()
                while (deadline > 0L) {
                    val remaining = deadline - System.nanoTime()
                    if (remaining > 0L) delay(remaining)
                    val next = channel.tryReceive().getOrNull()
                    if (next == null) {
                        onTimeout?.invoke(this@ReTimer); break
                    } else {
                        deadline = next // 延後，繼續內層迴圈
                    }
                }
            }
        }
    }

    /**
     * 重新倒數計時
     */
    fun restart() {
        val deadline = System.nanoTime() + intervalNanos
        channel.trySend(deadline)
        onStartOrRestart?.invoke(this@ReTimer)
    }

    /**
     * 取消倒數計時
     */
    fun cancel() {
        channel.trySend(0L)
    }
}