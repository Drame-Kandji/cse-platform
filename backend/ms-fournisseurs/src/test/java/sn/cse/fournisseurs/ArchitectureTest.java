package sn.cse.fournisseurs;

import com.tngtech.archunit.core.domain.JavaClasses;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import sn.cse.common.archunit.HexagonalArchitectureRules;

/** Vérifie les règles d'architecture hexagonale communes sur les classes du service. */
class ArchitectureTest {

  @TestFactory
  Stream<DynamicTest> regles() {
    JavaClasses classes = HexagonalArchitectureRules.classesDe("sn.cse.fournisseurs");
    return HexagonalArchitectureRules.toutes().stream()
        .map(r -> DynamicTest.dynamicTest(r.getDescription(), () -> r.check(classes)));
  }
}
