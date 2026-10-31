package sn.cse.audit;

import com.tngtech.archunit.core.domain.JavaClasses;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import sn.cse.common.archunit.HexagonalArchitectureRules;

/**
 * Règles d'architecture hexagonale communes, une règle par test dynamique (forme exécutée par
 * Surefire, voir docs/BACKEND_CONVENTIONS.md §5).
 */
class ArchitectureTest {

  @TestFactory
  Stream<DynamicTest> regles() {
    JavaClasses classes = HexagonalArchitectureRules.classesDe("sn.cse.audit");
    return HexagonalArchitectureRules.toutes().stream()
        .map(r -> DynamicTest.dynamicTest(r.getDescription(), () -> r.check(classes)));
  }
}
