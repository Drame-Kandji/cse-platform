package sn.cse.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertisseur JPA chiffrant une colonne texte au repos. Usage : {@code @Convert(converter =
 * EncryptedStringConverter.class)}. Instancié par Hibernate via le conteneur de beans Spring, qui
 * injecte l'{@link AesGcmEncryptor}.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

  private final AesGcmEncryptor encryptor;

  /**
   * @param encryptor chiffreur AES-GCM
   */
  public EncryptedStringConverter(AesGcmEncryptor encryptor) {
    this.encryptor = encryptor;
  }

  @Override
  public String convertToDatabaseColumn(String attribute) {
    return encryptor.chiffrer(attribute);
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    return encryptor.dechiffrer(dbData);
  }
}
