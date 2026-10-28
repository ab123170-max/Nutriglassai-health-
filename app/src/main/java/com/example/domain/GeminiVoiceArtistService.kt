package com.example.domain

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object GeminiVoiceArtistService {
    private const val ENDPOINT = "https://nutriglass-seven.vercel.app/api/voice"
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun generateWav(
        context: Context,
        text: String,
        style: String = "warm, natural audiobook narrator; clear pronunciation, expressive pauses, emotionally appropriate delivery",
        voice: String = "Kore"
    ): File = withContext(Dispatchers.IO) {
        require(text.isNotBlank()) { "There is no text to narrate." }
        if (text.length > 12000) throw IllegalArgumentException("Please generate narration for a shorter passage.")
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(("$voice|$style|$text").toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val directory = File(context.cacheDir, "ai_book_voice").apply { mkdirs() }
        val cached = File(directory, "$digest.wav")
        if (cached.exists() && cached.length() > 44) return@withContext cached

        val payload = JSONObject()
            .put("text", text)
            .put("style", style)
            .put("voice", voice)
            .toString()
        val request = Request.Builder()
            .url(ENDPOINT)
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        val response = try { client.newCall(request).execute() }
        catch (e: IOException) { throw IOException("Could not connect to AI voice service.", e) }

        response.use { res ->
            val raw = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                val message = try { JSONObject(raw).optString("error") } catch (_: Exception) { "" }
                throw IOException(message.ifBlank { "AI voice generation failed (HTTP ${res.code})." })
            }
            val encoded = JSONObject(raw).optString("audioBase64")
            if (encoded.isBlank()) throw IOException("AI voice service returned no audio.")
            val bytes = try { Base64.decode(encoded, Base64.DEFAULT) }
                catch (e: IllegalArgumentException) { throw IOException("AI voice audio was invalid.", e) }
            if (bytes.size < 44 || !bytes.copyOfRange(0, 4).contentEquals("RIFF".toByteArray())) {
                throw IOException("AI voice response was not a valid WAV file.")
            }
            val temp = File(directory, "$digest.tmp")
            temp.writeBytes(bytes)
            if (!temp.renameTo(cached)) {
                temp.copyTo(cached, overwrite = true)
                temp.delete()
            }
            cached
        }
    }
}
