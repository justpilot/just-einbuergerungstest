package io.justpilot.justeinbuergerung.ui

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object CatalogRoute

@Serializable
data object SettingsRoute

/** @param selection a [io.justpilot.justeinbuergerung.domain.learning.SessionSelection.key] */
@Serializable
data class SessionRoute(val selection: String)
