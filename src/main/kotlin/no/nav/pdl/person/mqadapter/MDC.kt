package no.nav.pdl.person.mqadapter

import java.io.Closeable
import kotlinx.coroutines.coroutineScope
import org.slf4j.MDC

const val X_CORRELATION_ID = "x-correlation-id"
const val NAV_CALL_ID = "nav-call-id"

suspend fun <R> withMDC(keyvalue: Pair<String, String>, block: () -> R): R {
    return MDC.putCloseable(keyvalue.first, keyvalue.second).use {
        block()
    }
}

suspend fun <R> withMDC(context: Map<String, String>, block: () -> R) = coroutineScope {
    CloseableMDCContext(context).use {
        block()
    }
}

private class CloseableMDCContext(newContext: Map<String, String>) : Closeable {
    private val originalContextMap = MDC.getCopyOfContextMap() ?: emptyMap()

    init {
        MDC.setContextMap(originalContextMap + newContext)
    }

    override fun close() {
        MDC.setContextMap(originalContextMap)
    }
}