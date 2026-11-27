
package com.wordnest.data.remote

import org.json.JSONObject

object ClozeExampleParser {

    fun parse(
        raw: String,
        word: String
    ): ClozeExample? {

        return runCatching {

            if (word.isBlank()) {
                return null
            }

            val clean = raw.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = JSONObject(clean)

            val example = json
                .optString("example")
                .trim()

            val translation = json
                .optString("translation")
                .trim()

            val blankCount =
                Regex("___").findAll(example).count()

            val answerPattern = Regex(
                "\\b${Regex.escape(word.trim())}\\b",
                RegexOption.IGNORE_CASE
            )

            if (
                example.isBlank() ||
                translation.isBlank() ||
                blankCount != 1 ||
                example.contains("____") ||
                answerPattern.containsMatchIn(example)
            ) {
                null
            } else {
                ClozeExample(
                    example = example,
                    translation = translation
                )
            }

        }.getOrNull()
    }
}
