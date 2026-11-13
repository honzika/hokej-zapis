package cz.hokej.zapis.history

import cz.hokej.zapis.model.Logo
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Loga podle názvu týmu. Vybrané logo se pamatuje, takže se příště doplní samo
 * a zobrazí se i u starších zápasů v historii (do záznamu zápasu se logo neukládá).
 */
@OptIn(ExperimentalEncodingApi::class)
class LogoLibrary(private val store: TextStore) {
    @Serializable
    private data class Entry(val mimeType: String, val base64: String)

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), Entry.serializer())
    private val entries: MutableMap<String, Entry> =
        (runCatching { store.read()?.let { json.decodeFromString(serializer, it) } }.getOrNull() ?: emptyMap()).toMutableMap()
    private val cache = mutableMapOf<String, Logo>()

    fun get(teamName: String): Logo? {
        val key = key(teamName)
        cache[key]?.let { return it }
        val e = entries[key] ?: return null
        return runCatching { Logo(Base64.decode(e.base64), e.mimeType) }.getOrNull()?.also { cache[key] = it }
    }

    /** null logo odebere. */
    fun put(teamName: String, logo: Logo?) {
        val key = key(teamName)
        if (key.isEmpty()) return
        cache.remove(key)
        if (logo == null) entries.remove(key) else {
            entries[key] = Entry(logo.mimeType, Base64.encode(logo.bytes))
            cache[key] = logo
        }
        store.write(json.encodeToString(serializer, entries))
    }

    private fun key(teamName: String) = teamName.trim().lowercase()
}
