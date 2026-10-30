package sn.cse.fournisseurs.archunit;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class HexagonalArchitectureTest {
    @Test
    void domainShouldNotDependOnFrameworks() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("sn.cse.fournisseurs");
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .orShould().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                .allowEmptyShould(true);
        rule.check(importedClasses);
    }
}
