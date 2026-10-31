package sn.cse.notification.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import sn.cse.notification.application.port.out.MessageEmail;
import sn.cse.notification.domain.model.NiveauNotification;
import sn.cse.notification.infrastructure.config.NotificationProperties;

class SmtpMailAdapterTest {

  private final JavaMailSender sender = mock(JavaMailSender.class);
  private final MessageEmail message =
      new MessageEmail(
          "daf@cse.sn",
          "Ousmane Ba",
          "Engagement à valider : CTR-2026-003",
          "L'engagement attend votre validation.",
          NiveauNotification.ALERTE,
          "/contrats/42");

  private SmtpMailAdapter adapter(boolean actif) {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    NotificationProperties props =
        new NotificationProperties(
            "http://localhost:5173/",
            "-",
            null,
            List.of(),
            new NotificationProperties.Mail(actif, "no-reply@cse.sn", "[CSE]"));
    return new SmtpMailAdapter(sender, engine, props);
  }

  @Test
  void should_send_french_html_mail_with_absolute_link() throws Exception {
    when(sender.createMimeMessage())
        .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    assertThat(adapter(true).envoyer(message)).isTrue();
    ArgumentCaptor<MimeMessage> envoye = ArgumentCaptor.forClass(MimeMessage.class);
    verify(sender).send(envoye.capture());
    MimeMessage mime = envoye.getValue();
    assertThat(mime.getSubject()).isEqualTo("[CSE] Engagement à valider : CTR-2026-003");
    assertThat(mime.getAllRecipients()[0]).hasToString("daf@cse.sn");
    String html = (String) mime.getContent();
    assertThat(html)
        .contains("Bonjour Ousmane Ba,")
        .contains("http://localhost:5173/contrats/42")
        .contains("Ouvrir dans la plateforme CSE")
        .contains("#d97706");
  }

  @Test
  void should_omit_button_when_no_link() {
    String html =
        adapter(true)
            .html(new MessageEmail("a@cse.sn", "A", "T", "M", NiveauNotification.INFO, null));
    assertThat(html).doesNotContain("Ouvrir dans la plateforme CSE").contains("#2563eb");
  }

  @Test
  void should_log_and_return_false_when_smtp_fails() {
    when(sender.createMimeMessage())
        .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    doThrow(new MailSendException("MailHog absent")).when(sender).send(any(MimeMessage.class));
    assertThat(adapter(true).envoyer(message)).isFalse();
  }

  @Test
  void should_not_send_when_disabled() {
    assertThat(adapter(false).envoyer(message)).isFalse();
    verify(sender, never()).createMimeMessage();
  }
}
