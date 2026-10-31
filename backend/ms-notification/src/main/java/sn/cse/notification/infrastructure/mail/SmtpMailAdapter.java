package sn.cse.notification.infrastructure.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import sn.cse.notification.application.port.out.MailPort;
import sn.cse.notification.application.port.out.MessageEmail;
import sn.cse.notification.infrastructure.config.NotificationProperties;

/**
 * Envoi SMTP (MailHog en développement) d'e-mails HTML mis en forme par Thymeleaf. Un échec est
 * journalisé et n'est jamais propagé : pas de rejeu Kafka pour un e-mail.
 */
@Component
public class SmtpMailAdapter implements MailPort {

  static final String MODELE = "mail/notification";
  private static final Logger LOG = LoggerFactory.getLogger(SmtpMailAdapter.class);

  private final JavaMailSender sender;
  private final ITemplateEngine templates;
  private final NotificationProperties properties;

  /**
   * @param sender client SMTP
   * @param templates moteur Thymeleaf
   * @param properties configuration
   */
  public SmtpMailAdapter(
      JavaMailSender sender, ITemplateEngine templates, NotificationProperties properties) {
    this.sender = sender;
    this.templates = templates;
    this.properties = properties;
  }

  @Override
  public boolean envoyer(MessageEmail message) {
    if (!properties.mail().actif()) {
      return false;
    }
    try {
      MimeMessage mime = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
      helper.setFrom(properties.mail().expediteur());
      helper.setTo(message.adresse());
      helper.setSubject(properties.mail().prefixeObjet() + " " + message.titre());
      helper.setText(html(message), true);
      sender.send(mime);
      return true;
    } catch (MessagingException | MailException e) {
      LOG.warn("Échec d'envoi de l'e-mail « {} » : {}", message.titre(), e.getMessage());
      LOG.debug("Détail de l'échec SMTP", e);
      return false;
    }
  }

  String html(MessageEmail message) {
    Context ctx = new Context(Locale.FRANCE);
    ctx.setVariable("nom", message.nomDestinataire());
    ctx.setVariable("titre", message.titre());
    ctx.setVariable("message", message.message());
    ctx.setVariable("niveau", message.niveau().name());
    ctx.setVariable("couleur", couleur(message));
    ctx.setVariable("lien", lienAbsolu(message.lien()));
    return templates.process(MODELE, ctx);
  }

  private String lienAbsolu(String lien) {
    if (lien == null || lien.isBlank()) {
      return null;
    }
    String base = properties.frontendBaseUrl();
    return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + lien;
  }

  private static String couleur(MessageEmail message) {
    return switch (message.niveau()) {
      case INFO -> "#2563eb";
      case SUCCES -> "#16a34a";
      case ALERTE -> "#d97706";
      case CRITIQUE -> "#dc2626";
    };
  }
}
