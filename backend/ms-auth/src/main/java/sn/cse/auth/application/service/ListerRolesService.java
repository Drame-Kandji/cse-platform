package sn.cse.auth.application.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.cse.auth.application.port.in.ListerRolesUseCase;
import sn.cse.auth.application.port.out.UtilisateurRepository;
import sn.cse.auth.application.query.RoleResume;
import sn.cse.auth.domain.model.PolitiqueOtp;
import sn.cse.auth.domain.model.Role;

/** Synthèse des rôles : effectifs et exigence d'OTP. */
@Service
@Transactional(readOnly = true)
public class ListerRolesService implements ListerRolesUseCase {

  private final UtilisateurRepository repository;
  private final PolitiqueOtp politiqueOtp;

  /**
   * @param repository profils
   * @param politiqueOtp rôles soumis à l'OTP
   */
  public ListerRolesService(UtilisateurRepository repository, PolitiqueOtp politiqueOtp) {
    this.repository = repository;
    this.politiqueOtp = politiqueOtp;
  }

  @Override
  public List<RoleResume> lister() {
    Map<Role, Long> effectifs = repository.compterParRole();
    return Arrays.stream(Role.values())
        .map(r -> new RoleResume(r, effectifs.getOrDefault(r, 0L), politiqueOtp.estObligatoire(r)))
        .toList();
  }
}
