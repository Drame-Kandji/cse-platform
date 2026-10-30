package sn.cse.common.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Clé de chiffrement des données sensibles ({@code cse.crypto.key}), fournie par variable
 * d'environnement ou secret, jamais versionnée.
 *
 * @param key clé AES-256 encodée en Base64 (32 octets)
 */
@ConfigurationProperties(prefix = "cse.crypto")
public record CryptoProperties(String key) {}
