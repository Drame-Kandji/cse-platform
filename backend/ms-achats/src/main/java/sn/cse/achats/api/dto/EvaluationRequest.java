package sn.cse.achats.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Notes saisies (PRIX et DELAI sont calculés automatiquement).
 *
 * @param qualiteTechnique note QUALITE_TECHNIQUE
 * @param garantie note GARANTIE
 * @param sav note SAV
 * @param references note REFERENCES
 */
@Schema(name = "EvaluationRequest", description = "Notes 0..100 des critères saisis")
public record EvaluationRequest(
    @JsonProperty("QUALITE_TECHNIQUE") @Schema(example = "80") @Min(0) @Max(100)
        Integer qualiteTechnique,
    @JsonProperty("GARANTIE") @Schema(example = "70") @Min(0) @Max(100) Integer garantie,
    @JsonProperty("SAV") @Schema(example = "65") @Min(0) @Max(100) Integer sav,
    @JsonProperty("REFERENCES") @Schema(example = "90") @Min(0) @Max(100) Integer references) {}
