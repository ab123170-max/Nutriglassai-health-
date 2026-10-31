package com.example.data.network

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Minimal Supabase REST/Auth client. The publishable key is a public client key;
 * authorization and row ownership are enforced by Supabase Auth + PostgreSQL RLS.
 * Never place a service-role/secret key in the Android app.
 */
object SupabaseClient {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.startsWith("https://") &&
            BuildConfig.SUPABASE_PUBLISHABLE_KEY.startsWith("sb_publishable_")

    private fun baseUrl(): String = BuildConfig.SUPABASE_URL.trimEnd('/')

    private fun request(path: String, accessToken: String? = null): Request.Builder {
        check(isConfigured) { "Supabase is not configured. Add SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY to the build secrets." }
        val builder = Request.Builder()
            .url("${baseUrl()}/$path")
            .header("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            .header("Accept", "application/json")
        builder.header("Authorization", "Bearer ${accessToken?.takeIf { it.isNotBlank() } ?: BuildConfig.SUPABASE_PUBLISHABLE_KEY}")
        return builder
    }

    suspend fun signUp(email: String, password: String): JSONObject = auth("signup", email, password)
    suspend fun signIn(email: String, password: String): JSONObject = auth("token?grant_type=password", email, password)

    private suspend fun auth(endpoint: String, email: String, password: String): JSONObject =
        withContext(Dispatchers.IO) {
            require(email.isNotBlank() && password.length >= 8) { "Enter a valid email and a password with at least 8 characters." }
            val body = JSONObject().put("email", email).put("password", password)
                .toString().toRequestBody("application/json".toMediaType())
            val response = http.newCall(
                request("auth/v1/$endpoint").post(body).build()
            ).execute()
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Supabase authentication failed (${response.code}): ${safeMessage(text)}")
            JSONObject(text)
        }

    /** Upsert one JSON record into health_records. Requires the signed-in user's access token. */
    suspend fun upsertRecord(accessToken: String, recordId: String, recordType: String, payload: JSONObject) =
        withContext(Dispatchers.IO) {
            require(accessToken.isNotBlank()) { "Please sign in before cloud backup." }
            val row = JSONObject()
                .put("record_id", recordId)
                .put("record_type", recordType)
                .put("payload", payload)
            val body = row.toString().toRequestBody("application/json".toMediaType())
            val response = http.newCall(
                request("rest/v1/health_records?on_conflict=user_id,record_id", accessToken)
                    .header("Content-Type", "application/json")
                    .header("Prefer", "resolution=merge-duplicates,return=minimal")
                    .post(body)
                    .build()
            ).execute()
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Cloud backup failed (${response.code}): ${safeMessage(text)}")
        }

    suspend fun fetchRecords(accessToken: String, recordType: String? = null): String =
        withContext(Dispatchers.IO) {
            require(accessToken.isNotBlank()) { "Please sign in before cloud restore." }
            val filter = recordType?.let { "&record_type=eq.${java.net.URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
            val response = http.newCall(
                request("rest/v1/health_records?select=record_id,record_type,payload,updated_at$filter&order=updated_at.desc", accessToken)
                    .get().build()
            ).execute()
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Cloud restore failed (${response.code}): ${safeMessage(text)}")
            text
        }

    suspend fun uploadHealthPhoto(accessToken: String, objectPath: String, bytes: ByteArray, contentType: String = "image/jpeg") =
        withContext(Dispatchers.IO) {
            require(accessToken.isNotBlank()) { "Please sign in before uploading photos." }
            val safePath = objectPath.split('/').joinToString("/") {
                java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20")
            }
            val body = bytes.toRequestBody(contentType.toMediaType())
            val response = http.newCall(
                request("storage/v1/object/health-photos/$safePath", accessToken)
                    .header("Content-Type", contentType)
                    .header("x-upsert", "true")
                    .post(body).build()
            ).execute()
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("Photo upload failed (${response.code}): ${safeMessage(text)}")
        }

    private fun safeMessage(body: String): String = try {
        JSONObject(body).optString("message",
            JSONObject(body).optString("msg", "Please check your Supabase configuration and RLS policies."))
    } catch (_: Exception) {
        "Please check your Supabase configuration and RLS policies."
    }
}
