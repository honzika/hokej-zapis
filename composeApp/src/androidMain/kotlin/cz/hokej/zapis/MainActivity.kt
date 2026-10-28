package cz.hokej.zapis

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import cz.hokej.zapis.cast.CastPublisher
import cz.hokej.zapis.ui.App

// AppCompatActivity kvůli MediaRouteButton (dialog výběru TV potřebuje FragmentManager a AppCompat téma).
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val publisher = CastPublisher.get(this)
        setContent { App(publisher) }
    }
}
