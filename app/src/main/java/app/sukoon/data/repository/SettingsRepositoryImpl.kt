package app.sukoon.data.repository

import app.sukoon.data.prefs.SettingsDataStore
import app.sukoon.domain.model.UserSettings
import app.sukoon.domain.repository.SecurityRepository
import app.sukoon.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val store: SettingsDataStore,
) : SettingsRepository {
    override val settings: Flow<UserSettings> = store.settings
    override suspend fun update(transform: (UserSettings) -> UserSettings) = store.update(transform)
    override suspend fun resetToDefaults() = store.resetPreservingState()
}

@Singleton
class SecurityRepositoryImpl @Inject constructor(
    private val store: SettingsDataStore,
) : SecurityRepository {
    override val hasPin: Flow<Boolean> = store.pinHash.map { it != null }

    override suspend fun setPin(pin: String) {
        val salt = PinHasher.newSalt()
        store.setPin(PinHasher.hash(pin, salt), salt)
    }

    override suspend fun verifyPin(pin: String): Boolean {
        val (hash, salt) = store.pinHash.first() ?: return false
        return PinHasher.verify(pin, salt, hash)
    }

    override suspend fun clearPin() = store.clearPin()
}

object PinHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256

    fun newSalt(): String {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), Base64.getDecoder().decode(salt), ITERATIONS, KEY_LENGTH)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            Base64.getEncoder().encodeToString(bytes)
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(pin: String, salt: String, expected: String): Boolean {
        val actual = hash(pin, salt).toByteArray()
        val exp = expected.toByteArray()
        if (actual.size != exp.size) return false
        var diff = 0
        for (i in actual.indices) diff = diff or (actual[i].toInt() xor exp[i].toInt())
        return diff == 0
    }
}
