package sn.cse.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AesGcmEncryptorTest {

  private static final String CLE = Base64.getEncoder().encodeToString(new byte[32]);
  private final AesGcmEncryptor encryptor = new AesGcmEncryptor(CLE);

  @Test
  void should_restore_plaintext_when_decrypting() {
    String chiffre = encryptor.chiffrer("SN08 SN010 01234567890123456789");

    assertThat(chiffre).doesNotContain("SN010");
    assertThat(encryptor.dechiffrer(chiffre)).isEqualTo("SN08 SN010 01234567890123456789");
  }

  @Test
  void should_use_random_iv_when_encrypting_twice() {
    assertThat(encryptor.chiffrer("245000000")).isNotEqualTo(encryptor.chiffrer("245000000"));
  }

  @Test
  void should_reject_tampered_data() {
    byte[] octets = Base64.getDecoder().decode(encryptor.chiffrer("montant"));
    octets[octets.length - 1] ^= 1;

    assertThatThrownBy(() -> encryptor.dechiffrer(Base64.getEncoder().encodeToString(octets)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void should_reject_key_when_not_256_bits() {
    String courte = Base64.getEncoder().encodeToString(new byte[16]);

    assertThatThrownBy(() -> new AesGcmEncryptor(courte))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void should_round_trip_amount_when_using_converter() {
    EncryptedBigDecimalConverter converter = new EncryptedBigDecimalConverter(encryptor);

    String colonne = converter.convertToDatabaseColumn(new BigDecimal("245000000"));

    assertThat(converter.convertToEntityAttribute(colonne)).isEqualByComparingTo("245000000");
    assertThat(converter.convertToDatabaseColumn(null)).isNull();
  }
}
