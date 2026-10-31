package com.example.finalwork1.data.sync

import com.example.finalwork1.domain.SyncPlanner
import kotlinx.coroutines.CancellationException

class SyncBatch(private val loadPage: suspend (Int) -> Boolean) {
    suspend fun run(
        cachedPages: Set<Int>, totalPages: Int, preloadCount: Int,
        onProgress: suspend (BatchProgress) -> Unit = {},
    ): BatchResult = runPages(
        SyncPlanner.pagesToPreload(cachedPages, totalPages, preloadCount), onProgress
    )

    suspend fun runPages(
        pages: List<Int>, onProgress: suspend (BatchProgress) -> Unit = {},
    ): BatchResult {
        val ordered = pages.distinct().sorted()
        var succeeded = 0
        var failed = 0
        onProgress(BatchProgress(0, ordered.size, 0, 0))
        for (page in ordered) {
            val ok = try { loadPage(page) }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { false }
            if (ok) succeeded++ else failed++
            onProgress(BatchProgress(succeeded + failed, ordered.size, succeeded, failed))
        }
        return BatchResult(succeeded, failed)
    }
}

data class BatchProgress(val completed: Int, val total: Int, val succeeded: Int, val failed: Int)
data class BatchResult(val succeeded: Int, val failed: Int) {
    val shouldRetry: Boolean get() = failed > 0
}
