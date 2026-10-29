package com.example.finalwork1.data.sync

import com.example.finalwork1.domain.SyncPlanner
import kotlinx.coroutines.CancellationException

class SyncBatch(private val loadPage: suspend (Int) -> Boolean) {
    suspend fun run(pageCount: Int): BatchResult = runPages(SyncPlanner.pagesToPreload(emptySet(), pageCount, pageCount))

    suspend fun runPages(pages: List<Int>): BatchResult {
        var succeeded = 0
        var failed = 0
        for (page in pages.distinct().sorted()) {
            val succeededPage = try { loadPage(page) } catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { false }
            if (succeededPage) succeeded++ else failed++
        }
        return BatchResult(succeeded, failed)
    }
}

data class BatchResult(val succeeded: Int, val failed: Int) {
    val shouldRetry: Boolean get() = failed > 0
}
