package com.github.berserkr2k.coreplugin.architecture

import com.github.berserkr2k.coreplugin.api.core.lifecycle.Feature
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaMethodCall
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent

@AnalyzeClasses(packages = ["com.github.berserkr2k.coreplugin"])
class ArchitectureTest {

    private fun getFeaturePackagePrefix(packageName: String): String? {
        val prefix = "com.github.berserkr2k.coreplugin.infrastructure"
        if (!packageName.startsWith(prefix)) return null
        val sub = packageName.substring(prefix.length).trimStart('.')
        val parts = sub.split('.')
        if (parts.isEmpty() || parts[0].isEmpty()) return null
        val firstPart = parts[0]
        
        val gameplayFeatures = setOf(
            "economy", "hologram", "warps", "utilitycommands", 
            "spawn", "kits", "scoreboard", "chat", "leaderboard"
        )
        if (firstPart in gameplayFeatures) {
            return "$prefix.$firstPart"
        }
        if (firstPart == "mechanics" && parts.size > 1) {
            val mechanicFeature = parts[1]
            val mechanicFeatures = setOf("shop", "trails")
            if (mechanicFeature in mechanicFeatures) {
                return "$prefix.mechanics.$mechanicFeature"
            }
        }
        return null
    }

    private fun getDeclaredDependenciesOfFeature(featureClassName: String): Set<String> {
        return try {
            val clazz = Class.forName(featureClassName)
            val feature = clazz.getDeclaredConstructor().newInstance() as Feature
            feature.descriptor.dependencies + feature.descriptor.optionalDependencies
        } catch (e: Exception) {
            emptySet()
        }
    }

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
    val feature_implementations_must_not_couple_directly: ArchRule = classes()
        .that().resideInAPackage("com.github.berserkr2k.coreplugin.infrastructure..")
        .should(object : ArchCondition<JavaClass>("not depend on other feature implementations directly") {
            override fun check(item: JavaClass, events: ConditionEvents) {
                val itemFeaturePackage = getFeaturePackagePrefix(item.packageName) ?: return
                for (dependency in item.directDependenciesFromSelf) {
                    val targetClass = dependency.targetClass
                    val targetFeaturePackage = getFeaturePackagePrefix(targetClass.packageName) ?: continue
                    if (itemFeaturePackage != targetFeaturePackage) {
                        val message = "Feature implementation class ${item.name} depends directly on " +
                                "another feature implementation class ${targetClass.name}. " +
                                "Communication must happen through public API contracts in com.github.berserkr2k.coreplugin.api!"
                        events.add(SimpleConditionEvent.violated(item, message))
                    }
                }
            }
        })

    @ArchTest
    val feature_listeners_must_not_subscribe_to_internal_events: ArchRule = classes()
        .that().resideInAPackage("com.github.berserkr2k.coreplugin.infrastructure..")
        .should(object : ArchCondition<JavaClass>("not subscribe to internal events of other features") {
            override fun check(item: JavaClass, events: ConditionEvents) {
                val itemFeaturePackage = getFeaturePackagePrefix(item.packageName) ?: return
                for (method in item.methods) {
                    val isEventHandler = method.annotations.any { 
                        it.rawType.name == "org.bukkit.event.EventHandler" 
                    }
                    if (!isEventHandler) continue
                    
                    val eventType = method.rawParameterTypes.firstOrNull() ?: continue
                    
                    val isInternalEvent = eventType.annotations.any {
                        it.rawType.name == "com.github.berserkr2k.coreplugin.api.core.event.InternalEvent"
                    }
                    if (!isInternalEvent) continue
                    
                    val eventFeaturePackage = getFeaturePackagePrefix(eventType.packageName) ?: continue
                    if (itemFeaturePackage != eventFeaturePackage) {
                        val message = "Method ${method.fullName} is an @EventHandler for internal event ${eventType.name} " +
                                "which belongs to feature package $eventFeaturePackage, but listener resides in $itemFeaturePackage!"
                        events.add(SimpleConditionEvent.violated(item, message))
                    }
                }
            }
        })

    @ArchTest
    val features_must_declare_dependencies: ArchRule = classes()
        .that().resideInAPackage("com.github.berserkr2k.coreplugin.infrastructure..")
        .should(object : ArchCondition<JavaClass>("declare dependencies on features whose API services they resolve") {
            override fun check(item: JavaClass, events: ConditionEvents) {
                val itemFeaturePackage = getFeaturePackagePrefix(item.packageName) ?: return
                val featureClassName = when {
                    itemFeaturePackage.endsWith("economy") -> "com.github.berserkr2k.coreplugin.infrastructure.economy.EconomyFeature"
                    itemFeaturePackage.endsWith("hologram") -> "com.github.berserkr2k.coreplugin.infrastructure.hologram.HologramFeature"
                    itemFeaturePackage.endsWith("warps") -> "com.github.berserkr2k.coreplugin.infrastructure.warps.WarpFeature"
                    itemFeaturePackage.endsWith("utilitycommands") -> "com.github.berserkr2k.coreplugin.infrastructure.utilitycommands.UtilityFeature"
                    itemFeaturePackage.endsWith("shop") -> "com.github.berserkr2k.coreplugin.infrastructure.mechanics.shop.ShopFeature"
                    itemFeaturePackage.endsWith("trails") -> "com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails.ProjectileTrailFeature"
                    itemFeaturePackage.endsWith("spawn") -> "com.github.berserkr2k.coreplugin.infrastructure.spawn.SpawnFeature"
                    itemFeaturePackage.endsWith("kits") -> "com.github.berserkr2k.coreplugin.infrastructure.kits.KitFeature"
                    itemFeaturePackage.endsWith("scoreboard") -> "com.github.berserkr2k.coreplugin.infrastructure.scoreboard.ScoreboardFeature"
                    itemFeaturePackage.endsWith("chat") -> "com.github.berserkr2k.coreplugin.infrastructure.chat.ChatFeature"
                    itemFeaturePackage.endsWith("leaderboard") -> "com.github.berserkr2k.coreplugin.infrastructure.leaderboard.LeaderboardFeature"
                    else -> null
                } ?: return

                val declaredDeps = getDeclaredDependenciesOfFeature(featureClassName).map { it.lowercase() }.toSet()

                val itemFeatureId = when {
                    itemFeaturePackage.endsWith("economy") -> "economy"
                    itemFeaturePackage.endsWith("hologram") -> "holograms"
                    itemFeaturePackage.endsWith("warps") -> "warps"
                    itemFeaturePackage.endsWith("utilitycommands") -> "utility-commands"
                    itemFeaturePackage.endsWith("shop") -> "shop"
                    itemFeaturePackage.endsWith("trails") -> "projectile-trails"
                    itemFeaturePackage.endsWith("spawn") -> "spawn"
                    itemFeaturePackage.endsWith("kits") -> "kits"
                    itemFeaturePackage.endsWith("scoreboard") -> "scoreboard"
                    itemFeaturePackage.endsWith("chat") -> "chat"
                    itemFeaturePackage.endsWith("leaderboard") -> "leaderboard"
                    else -> null
                }

                for (dependency in item.directDependenciesFromSelf) {
                    val targetClass = dependency.targetClass
                    val targetClassName = targetClass.name
                    val providerFeature = when (targetClassName) {
                        "com.github.berserkr2k.coreplugin.api.framework.economy.EconomyService" -> "economy"
                        "com.github.berserkr2k.coreplugin.api.feature.kits.KitService" -> "kits"
                        "com.github.berserkr2k.coreplugin.api.framework.warp.WarpService" -> "warps"
                        "com.github.berserkr2k.coreplugin.api.framework.hologram.HologramService" -> "holograms"
                        "com.github.berserkr2k.coreplugin.api.feature.leaderboard.LeaderboardService" -> "leaderboard"
                        "com.github.berserkr2k.coreplugin.api.feature.trails.ProjectileTrailService" -> "projectile-trails"
                        else -> null
                    }
                    if (providerFeature != null && providerFeature != itemFeatureId && providerFeature !in declaredDeps) {
                        val message = "Class ${item.name} uses service $targetClassName, " +
                                "but feature class $featureClassName does not declare dependency on '$providerFeature'!"
                        events.add(SimpleConditionEvent.violated(item, message))
                    }
                }
            }
        })

    @ArchTest
    val core_infra_should_not_leak_bukkit_internals: ArchRule = noClasses()
        .that().resideInAPackage("..com.github.berserkr2k.coreplugin.infrastructure.database..")
        .should().dependOnClassesThat().resideInAPackage("..org.bukkit..")

    @ArchTest
    val features_must_not_use_raw_bukkit_or_paper_messaging_leaks: ArchRule = noClasses()
        .that().resideInAPackage("..com.github.berserkr2k.coreplugin.infrastructure..")
        .and().resideOutsideOfPackage("..com.github.berserkr2k.coreplugin.infrastructure.message..")
        .and().resideOutsideOfPackage("..com.github.berserkr2k.coreplugin.infrastructure.lifecycle..")
        .should().callMethodWhere(object : DescribedPredicate<JavaMethodCall>("calls sendMessage or sendRawMessage") {
            override fun test(target: JavaMethodCall): Boolean {
                val name = target.name
                return name == "sendMessage" || name == "sendRawMessage"
            }
        })
        .`as`("Gameplay features must never invoke raw .sendMessage() or .sendRawMessage() on Bukkit entities. Use MessageService abstractions instead.")
}
