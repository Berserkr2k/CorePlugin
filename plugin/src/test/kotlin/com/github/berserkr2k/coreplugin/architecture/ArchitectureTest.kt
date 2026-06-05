package com.github.berserkr2k.coreplugin.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class ArchitectureTest {

    private fun inModule(moduleName: String): DescribedPredicate<JavaClass> {
        return object : DescribedPredicate<JavaClass>("classes in module $moduleName") {
            override fun test(javaClass: JavaClass): Boolean {
                val uri = javaClass.source.map { it.uri.toString().replace("\\", "/") }.orElse("")
                return uri.contains("/$moduleName/") || uri.contains("/$moduleName-")
            }
        }
    }

    private fun inAnyFeatureModule(): DescribedPredicate<JavaClass> {
        return object : DescribedPredicate<JavaClass>("classes in any feature module") {
            override fun test(javaClass: JavaClass): Boolean {
                val uri = javaClass.source.map { it.uri.toString().replace("\\", "/") }.orElse("")
                return uri.contains("/core-feature-")
            }
        }
    }

    @Test
    fun testModuleBoundaries() {
        val importedClasses = ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.github.berserkr2k.coreplugin..")

        // Sanity check to make sure classes are actually loaded from the classpath
        assert(importedClasses.size > 50) {
            "Expected to import more than 50 classes from the project, but only found ${importedClasses.size}!"
        }

        // 1. API cannot depend on Infra, Platform, or Features
        noClasses()
            .that(inModule("core-api"))
            .should()
            .dependOnClassesThat(
                inModule("core-infra")
                    .or(inModule("core-platform-api"))
                    .or(inModule("core-platform-paper"))
                    .or(inAnyFeatureModule())
            )
            .check(importedClasses)

        // 2. Platform (api & paper) cannot depend on Features
        noClasses()
            .that(inModule("core-platform-api").or(inModule("core-platform-paper")))
            .should()
            .dependOnClassesThat(inAnyFeatureModule())
            .check(importedClasses)

        // 3. Features cannot depend on other Features
        val features = listOf(
            "core-feature-economy",
            "core-feature-warps",
            "core-feature-holograms",
            "core-feature-leaderboard",
            "core-feature-kits",
            "core-feature-trails",
            "core-feature-shop",
            "core-feature-utility",
            "core-feature-chat"
        )

        for (feature in features) {
            val otherFeaturesPredicate = object : DescribedPredicate<JavaClass>("other features than $feature") {
                override fun test(javaClass: JavaClass): Boolean {
                    val uri = javaClass.source.map { it.uri.toString().replace("\\", "/") }.orElse("")
                    return uri.contains("/core-feature-") && !uri.contains("/$feature/")
                }
            }

            noClasses()
                .that(inModule(feature))
                .should()
                .dependOnClassesThat(otherFeaturesPredicate)
                .check(importedClasses)
        }
    }
}
