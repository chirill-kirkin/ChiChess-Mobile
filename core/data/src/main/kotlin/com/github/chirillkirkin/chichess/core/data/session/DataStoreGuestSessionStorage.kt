package com.github.chirillkirkin.chichess.core.data.session

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.github.chirillkirkin.chichess.core.data.network.chiChessJson
import com.github.chirillkirkin.chichess.core.domain.session.GuestSession
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import java.io.InputStream
import java.io.OutputStream

private const val GUEST_SESSION_FILE = "guest_session.json"

@Serializable
private data class StoredGuestSession(val sessionId: String, val token: String)

private object GuestSessionSerializer : Serializer<StoredGuestSession?> {
  override val defaultValue: StoredGuestSession? = null

  override suspend fun readFrom(input: InputStream): StoredGuestSession? {
    val bytes = input.readBytes()
    if (bytes.isEmpty()) return null
    return try {
      chiChessJson.decodeFromString(StoredGuestSession.serializer(), bytes.decodeToString())
    } catch (cause: SerializationException) {
      throw CorruptionException("Cannot read the stored guest session", cause)
    }
  }

  override suspend fun writeTo(t: StoredGuestSession?, output: OutputStream) {
    if (t == null) return
    output.write(chiChessJson.encodeToString(StoredGuestSession.serializer(), t).encodeToByteArray())
  }
}

private val Context.guestSessionDataStore: DataStore<StoredGuestSession?> by
  dataStore(fileName = GUEST_SESSION_FILE, serializer = GuestSessionSerializer)

class DataStoreGuestSessionStorage(private val context: Context) : GuestSessionStorage {
  override suspend fun read(): GuestSession? = context.guestSessionDataStore.data
    .first()
    ?.let { GuestSession(it.sessionId, it.token) }

  override suspend fun save(session: GuestSession) {
    context.guestSessionDataStore.updateData { StoredGuestSession(session.sessionId, session.token) }
  }
}
