package sn.cse.reporting.application.port.in;

import java.util.List;
import sn.cse.reporting.application.command.DemandeSerie;
import sn.cse.reporting.domain.model.Point;

/** Série temporelle d'un indicateur. */
public interface ConsulterSerieUseCase {

  /**
   * @param demande indicateur, granularité, période et chantier
   * @return les points
   */
  List<Point> serie(DemandeSerie demande);
}
