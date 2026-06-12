package com.github.berserkr2k.coreplugin.architecture

import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent

@AnalyzeClasses(packages = ["com.github.berserkr2k.coreplugin"])
class ArchitectureTest {

    @ArchTest
    val features_should_only_depend_on_api: ArchRule = classes()
        .that().resideInAPackage("..com.github.berserkr2k.coreplugin.infrastructure..")
        .and().haveSimpleNameEndingWith("Feature")
        .or().resideInAnyPackage("..core.feature..")
        .should(object : ArchCondition<JavaClass>("only depend on API, standard libraries, or classes within their own feature package") {
            override fun check(item: JavaClass, events: ConditionEvents) {
                val itemPackage = item.packageName
                for (dependency in item.directDependenciesFromSelf) {
                    val targetClass = dependency.targetClass
                    val targetPackage = targetClass.packageName
                    val allowed = targetPackage.startsWith("com.github.berserkr2k.coreplugin.api") ||
                            targetPackage.startsWith("java") ||
                            targetPackage.startsWith("kotlin") ||
                            targetPackage.startsWith("org.bukkit") ||
                            targetPackage.startsWith("net.kyori") ||
                            targetPackage.startsWith("org.slf4j") ||
                            targetPackage.startsWith("org.jetbrains.annotations") ||
                            targetPackage.startsWith(itemPackage)
                    if (!allowed) {
                        val message = "${item.name} depends on ${targetClass.name} which is outside allowed packages"
                        events.add(SimpleConditionEvent.violated(item, message))
                    }
                }
            }
        })

    @ArchTest
    val features_must_not_cross_couple: ArchRule = slices()
        .matching("com.github.berserkr2k.coreplugin.infrastructure.(*)..")
        .should().beFreeOfCycles()

    @ArchTest
    val core_infra_should_not_leak_bukkit_internals: ArchRule = noClasses()
        .that().resideInAPackage("..com.github.berserkr2k.coreplugin.infrastructure.database..")
        .should().dependOnClassesThat().resideInAPackage("..org.bukkit..")
}
