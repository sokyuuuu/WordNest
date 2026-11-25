package com.wordnest.data.dictionary

import com.wordnest.util.Constants
import com.wordnest.util.ExampleHole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

/** Kết quả phản hồi HTTP gọn nhẹ phục vụ tách biệt mạng thật và kiểm thử đơn vị. */
internal data class HttpResult(
    val statusCode: Int,
    val body: String,
)

internal fun interface HttpJsonFetcher {
    suspend fun get(url: String): HttpResult
}

/**
 * Bộ tải HTTP mặc định dùng [HttpURLConnection] chuẩn của Android (không thêm thư viện ngoài).
 */
internal class UrlConnectionJsonFetcher(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val connectTimeoutMs: Int = 5_000,
    private val readTimeoutMs: Int = 5_000,
) : HttpJsonFetcher {

    override suspend fun get(url: String): HttpResult = withContext(dispatcher) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "WordNest/1.1")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            HttpResult(statusCode = code, body = body)
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * Triển khai [DictionaryRepository] kết hợp 3 API công khai miễn phí (không cần API Key):
 *  1. **Datamuse API** (`api.datamuse.com/sug`) — gợi ý autocomplete từ tiếng Anh (> 150.000 mục từ).
 *  2. **Free Dictionary API** (`api.dictionaryapi.dev`) — phiên âm IPA chuẩn & câu ví dụ tiếng Anh.
 *  3. **MyMemory Translation API** (`api.mymemory.translated.net`) — dịch nghĩa tiếng Việt tự động.
 *
 * Kèm theo bộ nhớ đệm trong phiên và nguồn dự phòng nội bộ [OfflineDictionaryData] để vẫn gợi ý mượt
 * các từ thông dụng khi mất mạng mà không làm gián đoạn việc nhập tay.
 */
class OnlineDictionaryRepository internal constructor(
    private val fetcher: HttpJsonFetcher,
) : DictionaryRepository {

    constructor(dispatcher: CoroutineDispatcher = Dispatchers.IO) : this(
        fetcher = UrlConnectionJsonFetcher(dispatcher = dispatcher),
    )

    private data class FreeDictParsed(
        val phonetic: String?,
        val examples: List<String>,
    )

    private val suggestCache = ConcurrentHashMap<String, List<String>>()
    private val freeDictCache = ConcurrentHashMap<String, FreeDictParsed>()
    private val meaningCache = ConcurrentHashMap<String, List<String>>()

    override suspend fun suggest(prefix: String): List<String> {
        val normalized = prefix.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()
        suggestCache[normalized]?.let { return it }

        val offlineMatches = OfflineDictionaryData.suggest(normalized, MAX_SUGGESTIONS)
        val encoded = encodeQuery(normalized)
        val url = "$DATAMUSE_SUGGEST_URL?s=$encoded&max=$MAX_SUGGESTIONS"

        val onlineMatches = try {
            val response = fetcher.get(url)
            if (response.statusCode in 200..299 && response.body.isNotBlank()) {
                parseDatamuseSuggestions(response.body)
            } else {
                emptyList()
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (ioe: IOException) {
            if (offlineMatches.isNotEmpty()) return offlineMatches
            throw IOException("Không có kết nối mạng để tải gợi ý từ", ioe)
        } catch (_: Exception) {
            emptyList()
        }

        val merged = mergeCaseInsensitive(onlineMatches, offlineMatches)
            .filter { it.length <= Constants.MAX_WORD_TERM_LENGTH }
            .take(MAX_SUGGESTIONS)
        if (onlineMatches.isNotEmpty()) {
            suggestCache[normalized] = merged
        }
        return merged
    }

    override suspend fun getPhonetic(term: String): String? =
        lookupFreeDictionary(term).result.phonetic

    override suspend fun getExamples(term: String): List<String> =
        lookupFreeDictionary(term).result.examples

    override suspend fun getMeanings(term: String): List<String> =
        lookupVietnameseMeanings(term).result

    override suspend fun lookupDetails(term: String): WordSuggestionDetails {
        val cleanTerm = term.trim()
        if (cleanTerm.isEmpty()) return WordSuggestionDetails()

        return coroutineScope {
            val dictDeferred = async { lookupFreeDictionary(cleanTerm) }
            val transDeferred = async { lookupVietnameseMeanings(cleanTerm) }
            val dictOutcome = dictDeferred.await()
            val transOutcome = transDeferred.await()

            val phonetic = dictOutcome.result.phonetic
            val meanings = transOutcome.result
            val examples = dictOutcome.result.examples
            val hasAnyData = !phonetic.isNullOrBlank() || meanings.isNotEmpty() || examples.isNotEmpty()

            if (!hasAnyData && dictOutcome.networkFailed && transOutcome.networkFailed) {
                throw IOException("Không có kết nối mạng — bạn vẫn có thể nhập tay bình thường")
            }

            WordSuggestionDetails(
                phonetic = phonetic,
                meanings = meanings,
                examples = examples,
            )
        }
    }

    private data class FetchOutcome<T>(
        val result: T,
        val networkFailed: Boolean,
    )

    private suspend fun lookupFreeDictionary(term: String): FetchOutcome<FreeDictParsed> {
        val normalized = term.trim().lowercase()
        val offline = OfflineDictionaryData.find(normalized)
        if (normalized.isEmpty()) {
            return FetchOutcome(FreeDictParsed(null, emptyList()), networkFailed = false)
        }
        freeDictCache[normalized]?.let { cached ->
            return FetchOutcome(cached, networkFailed = false)
        }

        val url = "$FREE_DICTIONARY_BASE_URL/${encodePathSegment(normalized)}"
        return try {
            val response = fetcher.get(url)
            val online = when {
                response.statusCode in 200..299 && response.body.isNotBlank() ->
                    parseFreeDictionary(response.body, term.trim())

                // 404 = từ/cụm từ không có trong từ điển đơn ngữ (vẫn hợp lệ, dùng dự phòng nếu có)
                else -> FreeDictParsed(null, emptyList())
            }
            val merged = FreeDictParsed(
                phonetic = online.phonetic ?: offline?.phonetic,
                examples = prioritizeHoleCompatibleExamples(
                    examples = mergeCaseInsensitive(online.examples, offline?.examples.orEmpty()),
                    term = term.trim(),
                ),
            )
            if (response.statusCode in 200..299 || response.statusCode == 404) {
                freeDictCache[normalized] = merged
            }
            FetchOutcome(merged, networkFailed = false)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: IOException) {
            val fallback = FreeDictParsed(
                phonetic = offline?.phonetic,
                examples = offline?.examples.orEmpty().take(MAX_EXAMPLES),
            )
            FetchOutcome(fallback, networkFailed = offline == null)
        } catch (_: Exception) {
            val fallback = FreeDictParsed(
                phonetic = offline?.phonetic,
                examples = offline?.examples.orEmpty().take(MAX_EXAMPLES),
            )
            FetchOutcome(fallback, networkFailed = false)
        }
    }

    private suspend fun lookupVietnameseMeanings(term: String): FetchOutcome<List<String>> {
        val normalized = term.trim().lowercase()
        val offlineMeanings = OfflineDictionaryData.find(normalized)?.meanings.orEmpty()
        if (normalized.isEmpty()) {
            return FetchOutcome(emptyList(), networkFailed = false)
        }
        meaningCache[normalized]?.let { cached ->
            return FetchOutcome(cached, networkFailed = false)
        }

        val encoded = encodeQuery(term.trim())
        val url = "$MYMEMORY_TRANSLATE_URL?q=$encoded&langpair=en%7Cvi"
        return try {
            val response = fetcher.get(url)
            val onlineMeanings = if (response.statusCode in 200..299 && response.body.isNotBlank()) {
                parseMyMemoryMeanings(response.body, term.trim())
            } else {
                emptyList()
            }
            // Ưu tiên nghĩa tiếng Việt chọn lọc (nếu có cho từ cơ bản) rồi nối thêm các nghĩa dịch từ API
            val merged = mergeCaseInsensitive(offlineMeanings, onlineMeanings)
                .filter { it.length <= Constants.MAX_WORD_MEANING_LENGTH }
                .take(MAX_MEANINGS)
            if (onlineMeanings.isNotEmpty()) {
                meaningCache[normalized] = merged
            }
            FetchOutcome(merged, networkFailed = false)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: IOException) {
            FetchOutcome(
                result = offlineMeanings.take(MAX_MEANINGS),
                networkFailed = offlineMeanings.isEmpty(),
            )
        } catch (_: Exception) {
            FetchOutcome(
                result = offlineMeanings.take(MAX_MEANINGS),
                networkFailed = false,
            )
        }
    }

    private fun parseDatamuseSuggestions(json: String): List<String> {
        val array = JSONArray(json)
        val result = ArrayList<String>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val word = obj.optString("word").trim()
            if (word.isNotEmpty()) {
                result.add(word)
            }
        }
        return result
    }

    private fun parseFreeDictionary(json: String, term: String): FreeDictParsed {
        val root = JSONArray(json)
        var resolvedPhonetic: String? = null
        val rawExamples = mutableListOf<String>()

        for (i in 0 until root.length()) {
            val entry = root.optJSONObject(i) ?: continue
            if (resolvedPhonetic == null) {
                val direct = entry.optString("phonetic").trim()
                if (direct.isNotEmpty() && direct.length <= Constants.MAX_WORD_PHONETIC_LENGTH) {
                    resolvedPhonetic = direct
                } else {
                    val phoneticsArr = entry.optJSONArray("phonetics")
                    if (phoneticsArr != null) {
                        for (pIdx in 0 until phoneticsArr.length()) {
                            val pObj = phoneticsArr.optJSONObject(pIdx) ?: continue
                            val text = pObj.optString("text").trim()
                            if (text.isNotEmpty() && text.length <= Constants.MAX_WORD_PHONETIC_LENGTH) {
                                resolvedPhonetic = text
                                break
                            }
                        }
                    }
                }
            }

            val meaningsArr = entry.optJSONArray("meanings") ?: continue
            for (mIdx in 0 until meaningsArr.length()) {
                val meaningObj = meaningsArr.optJSONObject(mIdx) ?: continue
                val defsArr = meaningObj.optJSONArray("definitions") ?: continue
                for (dIdx in 0 until defsArr.length()) {
                    val defObj = defsArr.optJSONObject(dIdx) ?: continue
                    val ex = cleanSentence(defObj.optString("example"))
                    if (ex.isNotEmpty() && ex.length <= Constants.MAX_WORD_EXAMPLE_LENGTH) {
                        rawExamples.add(ex)
                    }
                }
            }
        }

        return FreeDictParsed(
            phonetic = resolvedPhonetic,
            examples = prioritizeHoleCompatibleExamples(rawExamples, term),
        )
    }

    private fun parseMyMemoryMeanings(json: String, originalTerm: String): List<String> {
        val root = JSONObject(json)
        val candidates = mutableListOf<String>()

        val responseData = root.optJSONObject("responseData")
        if (responseData != null) {
            cleanMeaningCandidate(responseData.optString("translatedText"), originalTerm)
                ?.let(candidates::add)
        }

        val matches = root.optJSONArray("matches")
        if (matches != null) {
            for (i in 0 until matches.length()) {
                val item = matches.optJSONObject(i) ?: continue
                val segment = item.optString("segment").trim()
                val matchScore = item.optDouble("match", 0.0)
                val quality = item.optString("quality", "70").toIntOrNull() ?: 70
                if (quality <= 0) continue
                val segmentMatches = segment.equals(originalTerm, ignoreCase = true) || matchScore >= 0.85
                if (!segmentMatches) continue

                cleanMeaningCandidate(item.optString("translation"), originalTerm)
                    ?.let(candidates::add)
            }
        }

        return mergeCaseInsensitive(candidates, emptyList()).take(MAX_MEANINGS)
    }

    private fun cleanMeaningCandidate(raw: String, originalTerm: String): String? {
        val decoded = decodeBasicHtmlEntities(raw)
            .trim()
            .trimEnd('.', ',', ';', '!', '?')
            .trim()
        if (decoded.isEmpty()) return null
        val upper = decoded.uppercase()
        if (upper.startsWith("MYMEMORY WARNING") || upper.startsWith("QUERY LENGTH LIMIT")) {
            return null
        }
        // Bỏ trường hợp API trả ngược lại y hệt từ tiếng Anh gốc khi không dịch được
        if (decoded.equals(originalTerm.trim(), ignoreCase = true)) {
            return null
        }
        return decoded
    }

    private fun cleanSentence(raw: String): String =
        decodeBasicHtmlEntities(raw)
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun decodeBasicHtmlEntities(text: String): String = text
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&amp;", "&")

    /**
     * Ưu tiên các câu ví dụ chứa nguyên từ gốc khớp được với [ExampleHole] để khi học Điền từ (F14)
     * câu ví dụ tự động hiển thị chỗ trống `___`.
     */
    private fun prioritizeHoleCompatibleExamples(examples: List<String>, term: String): List<String> {
        val unique = mergeCaseInsensitive(examples, emptyList())
        val (compatible, others) = unique.partition { ExampleHole.replace(it, term) != null }
        return (compatible + others).take(MAX_EXAMPLES)
    }

    private fun mergeCaseInsensitive(first: List<String>, second: List<String>): List<String> {
        val seen = HashSet<String>()
        val result = mutableListOf<String>()
        for (item in first + second) {
            val trimmed = item.trim()
            if (trimmed.isNotEmpty() && seen.add(trimmed.lowercase())) {
                result.add(trimmed)
            }
        }
        return result
    }

    private fun encodeQuery(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun encodePathSegment(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")

    companion object {
        internal const val DATAMUSE_SUGGEST_URL = "https://api.datamuse.com/sug"
        internal const val FREE_DICTIONARY_BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries/en"
        internal const val MYMEMORY_TRANSLATE_URL = "https://api.mymemory.translated.net/get"

        private const val MAX_SUGGESTIONS = 8
        private const val MAX_MEANINGS = 5
        private const val MAX_EXAMPLES = 3
    }
}
