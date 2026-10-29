package io.justpilot.justeinbuergerung.domain.model

/** The 16 German federal states; [code] is the prefix of their question ids, e.g. "BY-3". */
enum class FederalState(val code: String, val germanName: String) {
    BADEN_WUERTTEMBERG("BW", "Baden-Württemberg"),
    BAVARIA("BY", "Bayern"),
    BERLIN("BE", "Berlin"),
    BRANDENBURG("BB", "Brandenburg"),
    BREMEN("HB", "Bremen"),
    HAMBURG("HH", "Hamburg"),
    HESSE("HE", "Hessen"),
    MECKLENBURG_WESTERN_POMERANIA("MV", "Mecklenburg-Vorpommern"),
    LOWER_SAXONY("NI", "Niedersachsen"),
    NORTH_RHINE_WESTPHALIA("NW", "Nordrhein-Westfalen"),
    RHINELAND_PALATINATE("RP", "Rheinland-Pfalz"),
    SAARLAND("SL", "Saarland"),
    SAXONY("SN", "Sachsen"),
    SAXONY_ANHALT("ST", "Sachsen-Anhalt"),
    SCHLESWIG_HOLSTEIN("SH", "Schleswig-Holstein"),
    THURINGIA("TH", "Thüringen");

    companion object {
        fun fromCode(code: String): FederalState =
            entries.firstOrNull { it.code == code } ?: throw IllegalArgumentException("Unknown federal state code: $code")
    }
}
