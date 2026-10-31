package sn.cse.achats.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sn.cse.achats.api.dto.BesoinAAcheterDto;
import sn.cse.achats.api.mapper.AchatsApiMapper;
import sn.cse.achats.application.port.in.ConsulterAppelsOffresUseCase;
import sn.cse.common.pagination.PageResponse;
import sn.cse.common.web.PageableFactory;

/** Besoins transmis aux achats par ms-besoins (projection de BESOIN_EN_ACHAT). */
@RestController
@RequestMapping("/api/besoins-a-acheter")
@Tag(name = "Besoins à acheter", description = "Besoins orientés vers l'achat par le RM")
public class BesoinsAAcheterController {

  private static final Set<String> TRIABLES = Set.of("dateReception");

  private final ConsulterAppelsOffresUseCase consultation;
  private final PageableFactory pages;
  private final AchatsApiMapper mapper;

  /**
   * @param consultation lectures
   * @param pages pagination
   * @param mapper conversion DTO
   */
  public BesoinsAAcheterController(
      ConsulterAppelsOffresUseCase consultation, PageableFactory pages, AchatsApiMapper mapper) {
    this.consultation = consultation;
    this.pages = pages;
    this.mapper = mapper;
  }

  /**
   * @param page page
   * @param size taille
   * @return les besoins, les plus récents d'abord
   */
  @GetMapping
  @PreAuthorize(Acces.ACHATS_MATERIEL)
  @Operation(
      summary = "Lister les besoins à acheter",
      description = "Besoins reçus via BESOIN_EN_ACHAT, du plus récent au plus ancien.")
  @ApiResponse(responseCode = "200", description = "Page de besoins")
  public PageResponse<BesoinAAcheterDto> lister(
      @Parameter(example = "0") @RequestParam(required = false) Integer page,
      @Parameter(example = "20") @RequestParam(required = false) Integer size) {
    return consultation
        .besoinsAAcheter(pages.query(page, size, null, TRIABLES, "dateReception,desc"))
        .map(mapper::besoin);
  }
}
