package sn.cse.notification.infrastructure.config;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import sn.cse.notification.application.port.out.ReglesRoutageSource;
import sn.cse.notification.domain.model.ConditionRegle;
import sn.cse.notification.domain.model.RegleRoutage;

/** Adaptateur exposant les règles YAML validées sous forme de règles du domaine. */
@Component
public class ReglesRoutageConfiguration implements ReglesRoutageSource {

  private final List<RegleRoutage> regles;
  private final NotificationProperties properties;

  /**
   * @param properties configuration validée
   */
  public ReglesRoutageConfiguration(NotificationProperties properties) {
    this.properties = properties;
    this.regles = properties.regles().stream().map(ReglesRoutageConfiguration::convertir).toList();
  }

  @Override
  public List<RegleRoutage> regles() {
    return regles;
  }

  @Override
  public Map<String, String> libelles() {
    return properties.libelles();
  }

  @Override
  public String valeurAbsente() {
    return properties.valeurAbsente();
  }

  static RegleRoutage convertir(NotificationProperties.Regle r) {
    ConditionRegle condition =
        r.condition() == null
            ? null
            : new ConditionRegle(
                r.condition().champ(), r.condition().operateur(), r.condition().valeur());
    return new RegleRoutage(
        r.evenement(),
        Set.copyOf(r.roles()),
        r.champsDestinataires(),
        r.chefsDuChantier(),
        r.champChantier(),
        r.niveau(),
        r.titre(),
        r.message(),
        r.lien(),
        r.entite(),
        r.entiteId(),
        r.email(),
        condition);
  }
}
