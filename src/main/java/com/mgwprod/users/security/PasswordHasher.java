package com.mgwprod.users.security;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

// Convierte una contraseña en texto plano en algo seguro para guardar en la base, y
// después chequea un intento de login contra eso — sin nunca guardar la contraseña
// en texto plano.
@Component
public class PasswordHasher {

    private static final int SALT_LENGTH_BYTES = 16;

    // Se llama una sola vez, al registrarse. Genera un string tipo
    // "<saltBase64>:<hashBase64>" que va directo a User.passwordHash.
    public String hash(String rawPassword) {
        byte[] salt = generateSalt();
        byte[] hash = digest(rawPassword, salt);
        return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    }

    // Se llama en cada intento de login. Vuelve a hashear la contraseña recibida usando
    // el MISMO salt que ya estaba guardado, y compara los dos hashes — nunca se
    // desencripta nada, porque el hash es de un solo sentido (no reversible).
    public boolean matches(String rawPassword, String storedHash) {
        String[] parts = storedHash.split(":", 2);
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
        byte[] actualHash = digest(rawPassword, salt);
        // MessageDigest.isEqual en vez de Arrays.equals/== a propósito: compara en
        // tiempo constante, para que un atacante no pueda adivinar el hash byte por
        // byte midiendo cuánto tarda la comparación (ataque de timing).
        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    // Un salt aleatorio por usuario hace que dos usuarios con la misma contraseña
    // terminen con hashes completamente distintos — esto es lo que hace inútil un
    // ataque con tablas precalculadas ("rainbow tables") contra este hash.
    private byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    // Calcula SHA-256(salt + contraseña). Mismo input siempre da el mismo hash.
    private byte[] digest(String rawPassword, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(rawPassword.getBytes());
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es un algoritmo obligatorio en toda JVM, así que esta rama en la
            // práctica nunca se ejecuta — está solo porque la excepción checked obliga
            // a manejarla de alguna forma.
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
