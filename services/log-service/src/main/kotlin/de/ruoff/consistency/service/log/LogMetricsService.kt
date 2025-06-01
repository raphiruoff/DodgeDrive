//package de.ruoff.consistency.service.log
//
//import io.micrometer.core.instrument.MeterRegistry
//import org.springframework.stereotype.Service
//import java.time.Duration
//
//@Service
//class LogMetricsService(
//    private val meterRegistry: MeterRegistry
//) {
//
//    fun countLogEvent(eventType: String, username: String? = null) {
//        val counter = meterRegistry.counter("log_events_total", "event_type", eventType)
//        counter.increment()
//
//        if (username != null) {
//            meterRegistry.counter("log_events_by_user_total", "event_type", eventType, "user", username).increment()
//        }
//    }
//
//    fun countDuplicateEvent(eventType: String) {
//        meterRegistry.counter("log_event_duplicates_total", "event_type", eventType).increment()
//    }
//
//    fun countRetryAttempt(eventType: String) {
//        meterRegistry.counter("log_retry_attempts_total", "event_type", eventType).increment()
//    }
//
//}
