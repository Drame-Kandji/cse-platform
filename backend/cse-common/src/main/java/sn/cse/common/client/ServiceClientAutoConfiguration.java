package sn.cse.common.client;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import sn.cse.common.security.CurrentUserProvider;

/** Expose la fabrique de clients inter-services. */
@AutoConfiguration(after = RestClientAutoConfiguration.class)
@ConditionalOnClass(RestClient.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ServiceClientProperties.class)
public class ServiceClientAutoConfiguration {

  /**
   * @param builder builder Boot
   * @param properties délais
   * @return la fabrique
   */
  @Bean
  public ServiceRestClientFactory serviceRestClientFactory(
      RestClient.Builder builder, ServiceClientProperties properties) {
    return new ServiceRestClientFactory(builder, new CurrentUserProvider(), properties);
  }
}
