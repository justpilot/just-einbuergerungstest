package io.justpilot.justeinbuergerung.domain.model

/** Topic areas of the official catalog; [STATE] holds the ten questions of each federal state. */
enum class Category(val code: String) {
    POLITICS("politics"),
    HISTORY("history"),
    SOCIETY("society"),
    STATE("state");

    companion object {
        fun fromCode(code: String): Category =
            entries.firstOrNull { it.code == code } ?: throw IllegalArgumentException("Unknown category: $code")
    }
}

/** Languages of question texts. German is the exam language, the others are optional aids. */
enum class Language(val code: String) {
    GERMAN("de"),
    ENGLISH("en"),
    UKRAINIAN("uk");

    companion object {
        val translations = listOf(ENGLISH, UKRAINIAN)

        fun fromCode(code: String): Language =
            entries.firstOrNull { it.code == code } ?: throw IllegalArgumentException("Unknown language: $code")
    }
}

data class LocalizedText(val german: String, val english: String?, val ukrainian: String?) {
    operator fun get(language: Language): String? = when (language) {
        Language.GERMAN -> german
        Language.ENGLISH -> english
        Language.UKRAINIAN -> ukrainian
    }
}

data class AnswerOption(val id: String, val text: LocalizedText)

data class Question(
    /** "1"–"300" for general questions, "<state code>-<n>" for state questions. */
    val id: String,
    /** Number within the catalog: 1–300 for general questions, 1–10 within a state. */
    val number: Int,
    val state: FederalState?,
    val category: Category,
    /** Path of the picture inside the app's assets, or null. */
    val imagePath: String?,
    val text: LocalizedText,
    val options: List<AnswerOption>,
    val correctOptionId: String,
) {
    fun isCorrect(optionId: String): Boolean = optionId == correctOptionId
}
