package sn.cse.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;

/**
 * Convertisseur JPA chiffrant un montant au repos (colonne {@code TEXT}). Les agrégations se font
 * alors en mémoire côté service, jamais en SQL.
 */
@Converter
public class EncryptedBigDecimalConverter implements AttributeConverter<BigDecimal, String> {

  private final AesGcmEncryptor encryptor;

  /**
   * @param encryptor chiffreur AES-GCM
   */
  public EncryptedBigDecimalConverter(AesGcmEncryptor encryptor) {
    this.encryptor = encryptor;
  }

  @Override
  public String convertToDatabaseColumn(BigDecimal attribute) {
    return attribute == null ? null : encryptor.chiffrer(attribute.toPlainString());
  }

  @Override
  public BigDecimal convertToEntityAttribute(String dbData) {
    return dbData == null ? null : new BigDecimal(encryptor.dechiffrer(dbData));
  }
}
