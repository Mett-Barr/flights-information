package moozy.flightinformation.util.time

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
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
    private val channel = Channel<Long>(Channel.CONFLATED)
    private val intervalNanos = TimeUnit.MILLISECONDS.toNanos(intervalMillis)
    private var job: Job? = null

    private val _isCountingDown = MutableStateFlow(false)
    val isCountingDown: StateFlow<Boolean> get() = _isCountingDown

    init {
        startWatcher()
    }

    private fun startWatcher() {
        job = scope.launch  {
            try {
                while (true) {
                    var deadline = channel.receive()
                    while (deadline > 0L) {
                        val remainingNanos = deadline - System.nanoTime()
                        if (remainingNanos > 0L) {
                            val ms = TimeUnit.NANOSECONDS.toMillis(remainingNanos)
                            if (ms > 0L) delay(ms)
                            else yield() // 亞毫秒情況，讓出排程
                        }
                        // 確認是否被延後/取消
                        val next = channel.tryReceive().getOrNull()
                        if (next == null) {
                            // 沒有新指令 → 視為到期
                            _isCountingDown.value = false          // 視為結束倒數
                            onTimeout?.invoke(this@ReTimer); break
                        } else {
                            deadline = next // 延後，繼續內層迴圈
                        }
                    }
                    // 若外圈收到的是 0L（或內圈 break 後再回外圈），下一輪繼續等待
                }
            }  finally {
                // 協程終止時確保旗標歸零（避免 scope cancel 後殘留 true）
                _isCountingDown.value = false
            }
        }
    }

    /**
     * 重新倒數計時
     */
    fun restart() {
        val deadline = System.nanoTime() + intervalNanos
        if (channel.trySend(deadline).isSuccess) {
            _isCountingDown.value = true        // 視為開始倒數
            onStartOrRestart?.invoke(this@ReTimer)
        }
    }

    /**
     * 取消倒數計時
     */
    fun cancel() {
        if (channel.trySend(0L).isSuccess) {
            _isCountingDown.value = false       // 視為結束倒數
        }
    }
}