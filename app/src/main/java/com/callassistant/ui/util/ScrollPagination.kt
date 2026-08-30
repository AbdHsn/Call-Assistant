package com.callassistant.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

const val DEFAULT_PAGE_SIZE = 30

/**
 * Tracks how many items to show in a [LazyListState] list and grows the window
 * when the user scrolls near the bottom.
 *
 * @param totalItemCount Total items available (after filtering/sorting).
 * @param resetKey When this value changes, the visible window resets to [pageSize].
 * @return Number of items currently visible (capped at [totalItemCount]).
 */
@Composable
fun rememberScrollPagination(
    totalItemCount: Int,
    listState: LazyListState,
    resetKey: Any? = null,
    pageSize: Int = DEFAULT_PAGE_SIZE
): Int {
    var displayLimit by remember { mutableIntStateOf(pageSize) }

    LaunchedEffect(resetKey) {
        displayLimit = pageSize.coerceAtMost(totalItemCount)
    }

    LaunchedEffect(totalItemCount) {
        if (displayLimit > totalItemCount) {
            displayLimit = totalItemCount
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf false
            val lastVisible = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            lastVisible >= totalItems - 2
        }
    }

    LaunchedEffect(shouldLoadMore, totalItemCount) {
        if (shouldLoadMore && displayLimit < totalItemCount) {
            displayLimit = (displayLimit + pageSize).coerceAtMost(totalItemCount)
        }
    }

    return displayLimit
}
