package com.github.berserkr2k.coreplugin.architecture

import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaMethodCall
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import org.bukkit.entity.Player
import org.bukkit.command.CommandSender

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

    @ArchTest
    val features_must_not_use_raw_bukkit_or_paper_messaging_leaks: ArchRule = noClasses()
        .that().resideInAPackage("..com.github.berserkr2k.coreplugin.infrastructure..")
        .should().callMethodWhere(object : DescribedPredicate<JavaMethodCall>("calls sendMessage on Player or CommandSender") {
            override fun test(target: JavaMethodCall): Boolean {
                return target.name == "sendMessage" && 
                (target.target.owner.isAssignableTo(Player::class.java) || 
                 target.target.owner.isAssignableTo(CommandSender::class.java))
            }
        })
        .`as`("Gameplay features must never invoke raw .sendMessage() on Bukkit entities. Use MessageService abstractions instead.")
}
