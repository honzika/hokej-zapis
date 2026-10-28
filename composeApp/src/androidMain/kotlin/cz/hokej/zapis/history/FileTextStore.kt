package cz.hokej.zapis.history

import android.content.Context
import java.io.File

class FileTextStore(private val file: File) : TextStore {
    override fun read(): String? = file.takeIf { it.exists() }?.readText()

    override fun write(text: String) {
        // Zápis přes dočasný soubor, ať se při pádu nepřijde o celou historii.
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        tmp.renameTo(file)
    }

    companion object {
        @Volatile
        private var history: MatchHistory? = null

        fun history(context: Context): MatchHistory =
            history ?: synchronized(this) {
                history ?: MatchHistory(FileTextStore(File(context.filesDir, "matches.json"))).also { history = it }
            }
    }
}
