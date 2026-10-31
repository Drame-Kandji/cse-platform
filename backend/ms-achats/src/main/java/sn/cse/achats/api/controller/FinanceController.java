package sn.cse.achats.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.achats.api.dto.SyntheseFinanceDto;
import sn.cse.achats.api.mapper.AchatsApiMapper;
import sn.cse.achats.application.port.in.ConsulterFinanceUseCase;

/** Synthèse financière des achats. */
@RestController
@RequestMapping("/api/finance")
@Tag(name = "Finance", description = "Synthèse financière (DAF, DG)")
public class FinanceController {

  private final ConsulterFinanceUseCase consultation;
  private final AchatsApiMapper mapper;

  /**
   * @param consultation lectures
   * @param mapper conversion DTO
   */
  public FinanceController(ConsulterFinanceUseCase consultation, AchatsApiMapper mapper) {
    this.consultation = consultation;
    this.mapper = mapper;
  }

  /**
   * @return la synthèse
   */
  @GetMapping("/synthese")
  @PreAuthorize(Acces.FINANCE)
  @Operation(
      summary = "Synthèse financière",
      description =
          "Engagements validés et en attente, montants facturés, mandatés (mandatées + payées) et"
              + " payés (TTC), factures en retard.")
  @ApiResponse(responseCode = "200", description = "Synthèse")
  public SyntheseFinanceDto synthese() {
    return mapper.synthese(consultation.synthese());
  }
}
