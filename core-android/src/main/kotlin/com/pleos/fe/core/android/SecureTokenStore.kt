package com.pleos.fe.core.android

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pleos.fe.api.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 접근 토큰을 Android Keystore 키로 암호화해 DataStore에 저장한다(Pleos 보안 가이드: 키·토큰은 Keystore).
 * 인터셉터가 동기로 읽을 수 있게 메모리에 캐시하고, 쓰기는 백그라운드에서 한다.
 * 파일 하나당 인스턴스 하나만 만든다(앱 컨테이너에서 한 번).
 */
class SecureTokenStore(context: Context, fileName: String) : TokenStore {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cipher = KeystoreCipher(alias = "pleos_$fileName")
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { context.applicationContext.preferencesDataStoreFile(fileName) },
    )

    @Volatile
    private var cached: String? = runBlocking(Dispatchers.IO) {
        dataStore.data.first()[TOKEN]?.let(cipher::decryptOrNull)
    }

    override fun get(): String? = cached

    override fun set(token: String?) {
        cached = token
        scope.launch {
            dataStore.edit { prefs ->
                if (token == null) prefs.remove(TOKEN) else prefs[TOKEN] = cipher.encrypt(token)
            }
        }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
    }
}

/** AES-256-GCM. 저장 형식: Base64(IV 12바이트 + 암호문). 키를 잃으면(앱 재설치 등) null → 다시 가입·등록. */
internal class KeystoreCipher(private val alias: String) {

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val sealed = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(sealed, Base64.NO_WRAP)
    }

    fun decryptOrNull(stored: String): String? = runCatching {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, IV_SIZE))
        }
        String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE), Charsets.UTF_8)
    }.getOrNull()

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
    }
}
