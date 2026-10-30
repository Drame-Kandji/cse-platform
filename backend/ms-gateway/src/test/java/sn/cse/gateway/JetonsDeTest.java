package sn.cse.gateway;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/** Jetons RS256 signés par une clé de test, vérifiés par un décodeur remplaçant celui du JWKS. */
@TestConfiguration(proxyBeanMethods = false)
public class JetonsDeTest {

  private static final KeyPair CLES = generer();

  private static KeyPair generer() {
    try {
      KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
      generateur.initialize(2048);
      return generateur.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * @return décodeur vérifiant la signature de test
   */
  @Bean
  public ReactiveJwtDecoder decodeurDeTest() {
    return NimbusReactiveJwtDecoder.withPublicKey((RSAPublicKey) CLES.getPublic()).build();
  }

  /**
   * @param sub identifiant de l'utilisateur
   * @param roles rôles realm
   * @return un en-tête {@code Authorization} valide
   */
  public static String bearer(String sub, String... roles) {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(sub)
            .issuer("http://localhost:8180/realms/cse")
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now().plusSeconds(300)))
            .claim("realm_access", Map.of("roles", List.of(roles)))
            .build();
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
    try {
      jwt.sign(new RSASSASigner(CLES.getPrivate()));
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
    return "Bearer " + jwt.serialize();
  }
}
