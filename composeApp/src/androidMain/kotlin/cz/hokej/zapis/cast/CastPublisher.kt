package cz.hokej.zapis.cast

import android.content.Context
import android.util.Log
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import java.util.concurrent.Executors

/**
 * Posílá stav zápasu do připojeného Cast receiveru (receiver/index.html).
 * Volá se z hlavního vlákna (viewModelScope).
 */
class CastPublisher private constructor(context: Context) : ScoreboardPublisher {
    private var session: CastSession? = null
    private var latest: ScoreboardSnapshot? = null
    private var throttle = newThrottle()

    private val listener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(s: CastSession, sessionId: String) = attach(s)
        override fun onSessionResumed(s: CastSession, wasSuspended: Boolean) = attach(s)
        override fun onSessionEnded(s: CastSession, error: Int) = detach()
        override fun onSessionSuspended(s: CastSession, reason: Int) = detach()
        override fun onSessionStarting(s: CastSession) = Unit
        override fun onSessionStartFailed(s: CastSession, error: Int) = Unit
        override fun onSessionEnding(s: CastSession) = Unit
        override fun onSessionResuming(s: CastSession, sessionId: String) = Unit
        override fun onSessionResumeFailed(s: CastSession, error: Int) = Unit
    }

    init {
        CastContext.getSharedInstance(context.applicationContext, Executors.newSingleThreadExecutor())
            .addOnSuccessListener { ctx ->
                ctx.sessionManager.addSessionManagerListener(listener, CastSession::class.java)
                ctx.sessionManager.currentCastSession?.let(::attach)
            }
            .addOnFailureListener { Log.w(TAG, "Google Cast není k dispozici", it) }
    }

    override fun publish(snapshot: ScoreboardSnapshot) {
        latest = snapshot
        throttle.publish(snapshot)
    }

    private fun attach(s: CastSession) {
        Log.i(TAG, "Připojeno k ${s.castDevice?.friendlyName}, receiver ${s.applicationMetadata?.applicationId}")
        session = s
        // Nový receiver musí dostat aktuální stav hned, ne až při další změně.
        throttle = newThrottle()
        latest?.let(throttle::publish)
    }

    private fun detach() {
        Log.i(TAG, "Odpojeno od TV")
        session = null
    }

    private fun newThrottle() = ThrottledPublisher(::send)

    private fun send(snapshot: ScoreboardSnapshot) {
        val s = session ?: return
        s.sendMessage(CAST_NAMESPACE, snapshot.toJson()).setResultCallback { status ->
            if (status.isSuccess) Log.d(TAG, "Odesláno na TV: ${snapshot.home.score}:${snapshot.away.score} ${snapshot.phase}")
            else Log.w(TAG, "Odeslání na TV selhalo: ${status.statusCode} ${status.statusMessage}")
        }
    }

    companion object {
        private const val TAG = "CastPublisher"

        @Volatile
        private var instance: CastPublisher? = null

        fun get(context: Context): CastPublisher =
            instance ?: synchronized(this) { instance ?: CastPublisher(context).also { instance = it } }
    }
}
