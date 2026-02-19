package com.pramod.validator.data.models

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Generic paginated result wrapper for Firestore queries
 * @param items The list of items returned in this page
 * @param lastDocument The last document in this page (used for next page query)
 * @param hasMore Whether there are more items available to load
 */
data class PaginatedResult<T>(
    val items: List<T>,
    val lastDocument: DocumentSnapshot?,
    val hasMore: Boolean
)









