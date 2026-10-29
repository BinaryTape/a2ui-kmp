package ai.littlepress.a2ui.compat.java.util

// Recorded on an Android 15 emulator (API 35, ICU 75.1) from the digits java.text.NumberFormat prints
// (tests/integration, NumberingDump): the smallest table from which looking up language-script-region,
// language-script, language-region, then language (default "latn") gives Android's digits for every
// available locale and language x [script x] region. Do not edit; record again.
internal val ANDROID_NUMBERING: Map<String, String> = mapOf(
    "ar" to "arab",
    "as" to "beng",
    "bgc" to "deva",
    "bho" to "deva",
    "bn" to "beng",
    "ckb" to "arab",
    "dz" to "tibt",
    "fa" to "arabext",
    "ks" to "arabext",
    "lrc" to "arabext",
    "mni" to "beng",
    "mr" to "deva",
    "my" to "mymr",
    "mzn" to "arabext",
    "ne" to "deva",
    "nqo" to "nkoo",
    "ps" to "arabext",
    "raj" to "deva",
    "sa" to "deva",
    "sat" to "olck",
    "sd" to "arab",
    "ar-AE" to "latn",
    "ar-DZ" to "latn",
    "ar-EH" to "latn",
    "ar-LY" to "latn",
    "ar-MA" to "latn",
    "ar-TN" to "latn",
    "ar-XB" to "latn",
    "ks-Deva" to "latn",
    "pa-Arab" to "arabext",
    "sd-Deva" to "latn",
    "ur-IN" to "arabext",
    "uz-Arab" to "arabext",
    "pa-PK" to "arabext",
    "pa-Guru-PK" to "latn",
    "sd-IN" to "latn",
    "sd-Arab-IN" to "arab",
    "uz-AF" to "arabext",
    "uz-Cyrl-AF" to "latn",
    "uz-Latn-AF" to "latn",
)

/**
 * The default script of each language in [ANDROID_NUMBERING], recorded the same way. A locale with
 * another script does not inherit from its language ("mni-Mtei-IN" uses Latin digits, not "mni"'s).
 */
internal val ANDROID_DEFAULT_SCRIPTS: Map<String, String> = mapOf(
    "ar" to "Arab",
    "as" to "Beng",
    "bgc" to "Deva",
    "bho" to "Deva",
    "bn" to "Beng",
    "ckb" to "Arab",
    "dz" to "Tibt",
    "fa" to "Arab",
    "ks" to "Arab",
    "lrc" to "Arab",
    "mni" to "Beng",
    "mr" to "Deva",
    "my" to "Mymr",
    "mzn" to "Arab",
    "ne" to "Deva",
    "nqo" to "Nkoo",
    "pa" to "Guru",
    "ps" to "Arab",
    "raj" to "Deva",
    "sa" to "Deva",
    "sat" to "Olck",
    "sd" to "Arab",
    "ur" to "Arab",
    "uz" to "Latn",
)

/** The numbering system Android's java.text uses for a locale ("latn" when not in the tables). */
internal fun androidNumbering(language: String, script: String?, region: String?): String {
    val inheritsLanguage = script == null || script == ANDROID_DEFAULT_SCRIPTS[language]
    return listOfNotNull(
        if (script != null && region != null) "$language-$script-$region" else null,
        script?.let { "$language-$it" },
        if (region != null && inheritsLanguage) "$language-$region" else null,
        if (inheritsLanguage) language else null,
    ).firstNotNullOfOrNull { ANDROID_NUMBERING[it] } ?: "latn"
}
