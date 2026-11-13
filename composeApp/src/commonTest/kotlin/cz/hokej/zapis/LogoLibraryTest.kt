package cz.hokej.zapis

import cz.hokej.zapis.cast.toSnapshot
import cz.hokej.zapis.history.InMemoryTextStore
import cz.hokej.zapis.history.LogoLibrary
import cz.hokej.zapis.history.MatchRecord
import cz.hokej.zapis.history.TeamRecord
import cz.hokej.zapis.history.toState
import cz.hokej.zapis.model.Logo
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LogoLibraryTest {
    private val logo = Logo(byteArrayOf(1, 2, 3, -1), "image/webp")

    @Test
    fun logoSePamatujePodleNazvuIPoRestartu() {
        val store = InMemoryTextStore()
        LogoLibrary(store).put("  HC Lvi ", logo)

        val reloaded = LogoLibrary(store)
        val got = reloaded.get("hc lvi")!!
        assertContentEquals(logo.bytes, got.bytes)
        assertEquals("image/webp", got.mimeType)
        assertNull(reloaded.get("Vlci"))

        reloaded.put("HC LVI", null)
        assertNull(LogoLibrary(store).get("HC Lvi"))
    }

    @Test
    fun historieDoplniLogoAPosleHoNaTv() {
        val library = LogoLibrary(InMemoryTextStore()).apply { put("Lvi", logo) }
        val record = MatchRecord("1", 0, true, TeamRecord("Lvi", 0xFFD32F2F), TeamRecord("Vlci", 0xFF1565C0), 600, 3, emptyList())

        val snapshot = record.toState(library::get).toSnapshot()
        assertEquals("data:image/webp;base64,AQID/w==", snapshot.home.logo)
        assertNull(snapshot.away.logo)
    }

    @Test
    fun poskozenySouborNezpusobiPad() {
        assertNull(LogoLibrary(InMemoryTextStore("{nesmysl")).get("Lvi"))
    }
}
