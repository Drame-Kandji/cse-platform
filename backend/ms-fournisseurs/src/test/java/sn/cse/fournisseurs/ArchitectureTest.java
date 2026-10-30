package sn.cse.fournisseurs;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import sn.cse.common.archunit.HexagonalArchitectureRules;

@AnalyzeClasses(
    packages = "sn.cse.fournisseurs",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest static final ArchTests regles = ArchTests.in(HexagonalArchitectureRules.class);
}
