package dev.beneficios;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "dev.beneficios", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDoesNotDependOnOuterLayersOrFrameworks = noClasses()
            .that().resideInAPackage("dev.beneficios.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "dev.beneficios.ports..", "dev.beneficios.application..", "dev.beneficios.adapters..",
                    "dev.beneficios.config..", "org.springframework..", "io.github.resilience4j..", "org.mapstruct..");

    @ArchTest
    static final ArchRule portsDependOnlyOnDomain = noClasses()
            .that().resideInAPackage("dev.beneficios.ports..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "dev.beneficios.application..", "dev.beneficios.adapters..", "dev.beneficios.config..",
                    "org.springframework..", "io.github.resilience4j..", "org.mapstruct..");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnAdaptersOrFrameworks = noClasses()
            .that().resideInAPackage("dev.beneficios.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "dev.beneficios.adapters..", "dev.beneficios.config..",
                    "org.springframework..", "io.github.resilience4j..", "org.mapstruct..");

    @ArchTest
    static final ArchRule inboundAdaptersDoNotDependOnOutboundAdapters = noClasses()
            .that().resideInAPackage("dev.beneficios.adapters.inbound..")
            .should().dependOnClassesThat().resideInAPackage("dev.beneficios.adapters.outbound..");

    @ArchTest
    static final ArchRule outboundAdaptersDoNotDependOnInboundAdapters = noClasses()
            .that().resideInAPackage("dev.beneficios.adapters.outbound..")
            .should().dependOnClassesThat().resideInAPackage("dev.beneficios.adapters.inbound..");

    @ArchTest
    static final ArchRule adaptersDoNotDependOnConfig = noClasses()
            .that().resideInAPackage("dev.beneficios.adapters..")
            .should().dependOnClassesThat().resideInAPackage("dev.beneficios.config..");
}
