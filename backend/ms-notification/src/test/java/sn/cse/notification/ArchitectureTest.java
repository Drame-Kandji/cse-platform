package sn.cse.notification;

import com.tngtech.archunit.core.domain.JavaClasses;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import sn.cse.common.archunit.HexagonalArchitectureRules;

class ArchitectureTest {

  @TestFactory
  Stream<DynamicTest> regles() {
    JavaClasses classes = HexagonalArchitectureRules.classesDe("sn.cse.notification");
    return HexagonalArchitectureRules.toutes().stream()
        .map(r -> DynamicTest.dynamicTest(r.getDescription(), () -> r.check(classes)));
  }
}
