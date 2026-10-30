package sn.cse.common.crypto;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** Active le chiffrement AES-256-GCM dès que {@code cse.crypto.key} est fournie. */
@AutoConfiguration
@EnableConfigurationProperties(CryptoProperties.class)
@ConditionalOnProperty(prefix = "cse.crypto", name = "key")
public class CryptoAutoConfiguration {

  /**
   * @param properties clé de chiffrement
   * @return le chiffreur
   */
  @Bean
  public AesGcmEncryptor aesGcmEncryptor(CryptoProperties properties) {
    return new AesGcmEncryptor(properties.key());
  }
}
