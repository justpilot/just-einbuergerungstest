package io.justpilot.justeinbuergerung.ui

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object CatalogRoute

@Serializable
data object ExamRoute

@Serializable
data object SettingsRoute

/** A running exam simulation. */
@Serializable
data object ExamSessionRoute

/** @param selection a [io.justpilot.justeinbuergerung.domain.learning.SessionSelection.key] */
@Serializable
data class SessionRoute(val selection: String)
