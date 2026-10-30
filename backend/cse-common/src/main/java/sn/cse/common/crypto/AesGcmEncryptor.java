package sn.cse.common.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Chiffrement authentifié AES-256-GCM. Format stocké : Base64(IV 12 octets || texte chiffré + tag).
 * Un IV aléatoire est tiré à chaque chiffrement (jamais réutilisé avec la même clé).
 */
public class AesGcmEncryptor {

  private static final String ALGO = "AES/GCM/NoPadding";
  private static final int TAG_BITS = 128;
  private static final int IV_OCTETS = 12;
  private static final int CLE_OCTETS = 32;

  private final SecretKeySpec cle;
  private final SecureRandom random = new SecureRandom();

  /**
   * @param base64Key clé AES-256 en Base64
   * @throws IllegalArgumentException si la clé est absente ou ne fait pas 256 bits
   */
  public AesGcmEncryptor(String base64Key) {
    if (base64Key == null || base64Key.isBlank()) {
      throw new IllegalArgumentException("cse.crypto.key est obligatoire (clé AES-256 en Base64)");
    }
    byte[] octets = Base64.getDecoder().decode(base64Key.trim());
    if (octets.length != CLE_OCTETS) {
      throw new IllegalArgumentException("cse.crypto.key doit faire 256 bits (32 octets)");
    }
    this.cle = new SecretKeySpec(octets, "AES");
  }

  /**
   * @param clair texte en clair (null autorisé)
   * @return texte chiffré en Base64, ou null
   */
  public String chiffrer(String clair) {
    if (clair == null) {
      return null;
    }
    try {
      byte[] iv = new byte[IV_OCTETS];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(ALGO);
      cipher.init(Cipher.ENCRYPT_MODE, cle, new GCMParameterSpec(TAG_BITS, iv));
      byte[] chiffre = cipher.doFinal(clair.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder()
          .encodeToString(
              ByteBuffer.allocate(iv.length + chiffre.length).put(iv).put(chiffre).array());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Échec du chiffrement AES-GCM", e);
    }
  }

  /**
   * @param base64 texte chiffré (null autorisé)
   * @return texte en clair, ou null
   * @throws IllegalStateException si la donnée a été altérée ou la clé est incorrecte
   */
  public String dechiffrer(String base64) {
    if (base64 == null) {
      return null;
    }
    try {
      ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(base64));
      byte[] iv = new byte[IV_OCTETS];
      buffer.get(iv);
      byte[] chiffre = new byte[buffer.remaining()];
      buffer.get(chiffre);
      Cipher cipher = Cipher.getInstance(ALGO);
      cipher.init(Cipher.DECRYPT_MODE, cle, new GCMParameterSpec(TAG_BITS, iv));
      return new String(cipher.doFinal(chiffre), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Échec du déchiffrement AES-GCM (donnée altérée ?)", e);
    }
  }
}
