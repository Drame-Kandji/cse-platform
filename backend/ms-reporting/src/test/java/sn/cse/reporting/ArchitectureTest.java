package sn.cse.reporting;

import com.tngtech.archunit.core.domain.JavaClasses;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import sn.cse.common.archunit.HexagonalArchitectureRules;

/**
 * Règles d'architecture hexagonale communes, une règle par test dynamique (forme exécutée par
 * Surefire, voir docs/BACKEND_CONVENTIONS.md §5). Les vues de ms-reporting étant persistées en JDBC
 * (aucune entité JPA), les règles acceptent un ensemble vide.
 */
class ArchitectureTest {

  @TestFactory
  Stream<DynamicTest> regles() {
    JavaClasses classes = HexagonalArchitectureRules.classesDe("sn.cse.reporting");
    return HexagonalArchitectureRules.toutes().stream()
        .map(
            r ->
                DynamicTest.dynamicTest(
                    r.getDescription(),
                    // Vues persistées en JDBC : la règle sur les entités JPA ne trouve aucune
                    // classe.
                    () -> r.allowEmptyShould(true).check(classes)));
  }
}
