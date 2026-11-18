package com.example.ludo

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import ludo.engine.Color

enum class Sfx { ROLL, STEP, CAPTURE, HOME, WIN }

private val PITCH = mapOf(Color.RED to 1f, Color.GREEN to 1.26f, Color.YELLOW to .8f, Color.BLUE to 1.5f)

class SoundFx(context: Context) {
    var enabled = true
    private val pool = SoundPool.Builder().setMaxStreams(4)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val ids = mapOf(
        Sfx.ROLL to R.raw.roll, Sfx.STEP to R.raw.step, Sfx.CAPTURE to R.raw.capture, Sfx.HOME to R.raw.home, Sfx.WIN to R.raw.win,
    ).mapValues { (_, res) -> pool.load(context, res, 1) }

    /** Each player gets the same sounds at a different pitch (playback rate), so you can hear whose move it is. */
    fun play(s: Sfx, color: Color? = null) {
        if (enabled) pool.play(ids.getValue(s), 1f, 1f, 1, 0, color?.let { PITCH.getValue(it) } ?: 1f)
    }
    fun release() = pool.release()
}

/** Looping background music, created lazily so it costs nothing when switched off. */
class Music(private val context: Context) {
    private var player: MediaPlayer? = null

    fun start() {
        if (player == null) player = MediaPlayer.create(context, R.raw.music)?.apply { isLooping = true; setVolume(.12f, .12f) }
        player?.start()
    }
    fun pause() { player?.takeIf { it.isPlaying }?.pause() }
    fun release() { player?.release(); player = null }
}
