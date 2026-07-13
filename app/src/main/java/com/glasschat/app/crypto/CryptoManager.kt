package com.glasschat.app.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(org.bouncycastle.jce.provider.BouncyCastleProvider())
        }
    }

    private const val AES_KEY_SIZE_BITS = 256
    private const val GCM_TAG_SIZE_BITS = 128
    private const val GCM_NONCE_SIZE_BYTES = 12

    data class X25519KeyPair(val privateKey: PrivateKey, val publicKey: PublicKey)

    fun generateX25519KeyPair(): X25519KeyPair {
        val kpg = KeyPairGenerator.getInstance("X25519", "BC")
        val kp = kpg.generateKeyPair()
        return X25519KeyPair(kp.private, kp.public)
    }

    fun deriveSharedSecret(myPrivateKey: PrivateKey, peerPublicKey: PublicKey): ByteArray {
        val ka = KeyAgreement.getInstance("X25519", "BC")
        ka.init(myPrivateKey)
        ka.doPhase(peerPublicKey, true)
        return ka.generateSecret()
    }

    fun hkdf(ikm: ByteArray, salt: ByteArray, info: ByteArray, outputLength: Int = 32): ByteArray {
        val prk = hmacSha256(salt, ikm)
        var t = ByteArray(0)
        val output = ByteArray(outputLength)
        var generated = 0
        var counter = 1
        while (generated < outputLength) {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(t)
            mac.update(info)
            mac.update(counter.toByte())
            t = mac.doFinal()
            val toCopy = minOf(t.size, outputLength - generated)
            System.arraycopy(t, 0, output, generated, toCopy)
            generated += toCopy
            counter++
        }
        return output
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.ifEmpty { ByteArray(32) }, "HmacSHA256"))
        return mac.doFinal(data)
    }

    data class EncryptedPayload(val nonce: ByteArray, val ciphertext: ByteArray)

    fun encrypt(key: ByteArray, plaintext: ByteArray, aad: ByteArray = ByteArray(0)): EncryptedPayload {
        require(key.size == AES_KEY_SIZE_BITS / 8) { "AES-256 key must be 32 bytes" }
        val nonce = ByteArray(GCM_NONCE_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(GCM_TAG_SIZE_BITS, nonce)
        )
        if (aad.isNotEmpty()) cipher.updateAAD(aad)
        val ciphertext = cipher.doFinal(plaintext)
        return EncryptedPayload(nonce, ciphertext)
    }

    fun decrypt(key: ByteArray, payload: EncryptedPayload, aad: ByteArray = ByteArray(0)): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(GCM_TAG_SIZE_BITS, payload.nonce)
        )
        if (aad.isNotEmpty()) cipher.updateAAD(aad)
        return cipher.doFinal(payload.ciphertext)
    }

    fun deriveKeyFromPassphrase(passphrase: CharArray, salt: ByteArray): ByteArray {
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withParallelism(2)
            .withMemoryAsKB(65536)
            .withIterations(3)
            .build()
        val generator = Argon2BytesGenerator()
        generator.init(params)
        val out = ByteArray(32)
        generator.generateBytes(passphrase.concatToString().toByteArray(Charsets.UTF_8), out)
        return out
    }

    fun randomSalt(size: Int = 16): ByteArray = ByteArray(size).also { SecureRandom().nextBytes(it) }
}
