package de.ruoff.consistency.service.log

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class LogMetricsService(
    private val meterRegistry: MeterRegistry
) {

    fun countLogEvent(eventType: String, username: String? = null) {
        val counter = meterRegistry.counter("log_events_total", "event_type", eventType)
        counter.increment()

        if (username != null) {
            meterRegistry.counter("log_events_by_user_total", "event_type", eventType, "user", username).increment()
        }
    }

    fun recordLatency(eventType: String, delayMs: Long) {
        val summary = meterRegistry.summary("log_event_latency_ms", "event_type", eventType)
        summary.record(delayMs.toDouble())

        val timer = meterRegistry.timer("log_event_latency_timer", "event_type", eventType)
        timer.record(Duration.ofMillis(delayMs))
    }

    fun countDuplicateEvent(eventType: String) {
        meterRegistry.counter("log_event_duplicates_total", "event_type", eventType).increment()
    }

    fun countExport(gameId: String) {
        meterRegistry.counter("log_exports_total", "game_id", gameId).increment()
    }
}
