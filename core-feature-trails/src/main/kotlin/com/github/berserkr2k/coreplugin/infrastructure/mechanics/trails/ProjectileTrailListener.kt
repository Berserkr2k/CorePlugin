package com.github.berserkr2k.coreplugin.infrastructure.mechanics.trails

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import com.destroystokyo.paper.ParticleBuilder
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector

import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import com.github.berserkr2k.coreplugin.api.core.scheduler.TaskScheduler

class ProjectileTrailListener(
    private val plugin: Plugin,
    private val trailManager: ProjectileTrailManager,
    private val taskScheduler: TaskScheduler
) : Listener {
 
    private val trailKey = NamespacedKey(plugin, "projectile_trail_id")

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val proj = event.entity
        
        // Solo aplicar estelas a proyectiles de armas (Bows, Crossbows, Tridents), no huevos, bolas de nieve, etc.
        if (proj !is org.bukkit.entity.AbstractArrow) {
            return
        }

        val shooter = proj.shooter as? Player

        // 1. Resolver qué estela aplicar (prioridad: PDC del proyectil > cosmético del jugador)
        val pdcTrailId = proj.persistentDataContainer.get(trailKey, PersistentDataType.STRING)
        val playerTrailId = if (shooter != null) trailManager.getActiveTrail(shooter.uniqueId) else null
        val finalTrailId = pdcTrailId ?: playerTrailId ?: return

        val config = trailManager.trails[finalTrailId.lowercase()] ?: return

        // 1.5. Verificar permisos del lanzador en tiempo real
        if (shooter != null && !shooter.hasPermission(config.permission)) {
            return
        }

        // 2. Iniciar seguimiento regional seguro (Folia-ready)
        var prevLoc = proj.location.clone()
        var angleState = 0.0
        var tickCounter = 0

        // El planificador de la entidad garantiza que se ejecute en el hilo regional correcto
        proj.scheduler.runAtFixedRate(plugin, { task ->
            if (!proj.isValid || proj.isDead || proj.isOnGround || proj.location.y < proj.world.minHeight - 64.0 || tickCounter > 600) {
                task.cancel()
                return@runAtFixedRate
            }

            tickCounter++
            val currLoc = proj.location
            val velocity = proj.velocity
            val velLength = velocity.length()

            // 2.2. Verificar condiciones dinámicas antes de emitir partículas
            if (!checkConditions(shooter, proj, config.conditions)) {
                return@runAtFixedRate
            }

            // Calcular intervalo de ticks adaptativo según MSPT y velocidad
            val interval = trailManager.getDynamicInterval(config.baseInterval, velLength)

            if (tickCounter % interval != 0) {
                return@runAtFixedRate
            }

            // Evaluar emisión en ráfagas (BURST)
            val isBurstMode = config.emissionMode.uppercase() == "BURST"
            val activeBurstCount = if (isBurstMode) {
                if (tickCounter % (config.burstEvery * interval) == 0) {
                    config.burstCount
                } else {
                    0
                }
            } else {
                config.particleCount
            }

            // Si es modo ráfaga y este tick no toca ráfaga, saltar
            if (isBurstMode && activeBurstCount == 0) {
                return@runAtFixedRate
            }

            // Capturar estado atómico para pasar al hilo asíncrono sin peligro de concurrencia
            val pPrev = prevLoc.clone()
            val pCurr = currLoc.clone()
            val dir = if (velLength > 0.001) velocity.clone().normalize() else Vector(0.0, 1.0, 0.0)
            val currentAngle = angleState

            // Actualizar referencias regionales
            prevLoc = pCurr.clone()
            angleState += velLength * config.spiralSpeed

            // 3. Delegar los cálculos trigonométricos pesados y la creación de paquetes al hilo asíncrono
            taskScheduler.runAsync {
                val dist = pPrev.distance(pCurr)
                if (dist < 0.05) {
                    spawnParticleAt(pCurr, config, currentAngle, dir, activeBurstCount, velLength)
                } else {
                    // Interpolación espacial basada en la densidad (partículas por bloque)
                    val particleSteps = Math.max(1, (dist * config.density).toInt())
                    for (i in 0 until particleSteps) {
                        val fraction = i.toDouble() / particleSteps
                        val interpolatedLoc = pPrev.clone().add(pCurr.clone().subtract(pPrev).multiply(fraction))
                        val stepDist = dist * fraction
                        val interpolatedAngle = currentAngle + (stepDist * config.spiralSpeed)
                        
                        spawnParticleAt(interpolatedLoc, config, interpolatedAngle, dir, activeBurstCount, velLength)
                    }
                }
            }
        }, null, 1L, 1L)
    }

    private fun spawnParticleAt(
        loc: org.bukkit.Location,
        config: TrailConfig,
        angle: Double,
        dir: Vector,
        activeCount: Int,
        velLength: Double
    ) {
        // 1. Renderizar partícula base
        spawnSingleParticle(
            loc, config, angle, dir,
            config.particleType, activeCount, config.speed,
            config.spiralRadius, config.offsetX, config.offsetY, config.offsetZ, velLength
        )

        // 2. Renderizar capas adicionales configuradas (layers)
        config.layers.forEach { layer ->
            val layerCount = if (config.emissionMode.uppercase() == "BURST") {
                // Escalar proporcionalmente al tamaño de ráfaga activo
                ((layer.count.toDouble() / config.particleCount.coerceAtLeast(1)) * activeCount).toInt().coerceAtLeast(1)
            } else {
                layer.count
            }

            spawnSingleParticle(
                loc, config, angle, dir,
                layer.particle, layerCount, layer.speed,
                layer.radius, layer.offsetX, layer.offsetY, layer.offsetZ, velLength
            )
        }
    }

    private fun spawnSingleParticle(
        loc: org.bukkit.Location,
        config: TrailConfig,
        angle: Double,
        dir: Vector,
        particleTypeStr: String,
        count: Int,
        speed: Double,
        radius: Double,
        offsetX: Double,
        offsetY: Double,
        offsetZ: Double,
        velLength: Double
    ) {
        val particleType = try {
            Particle.valueOf(particleTypeStr.uppercase())
        } catch (e: Exception) {
            Particle.FLAME
        }

        // --- APLICACIÓN DE MODIFICADORES ---

        // 1. randomness: añadir ruido de dispersión aleatoria
        val randomOffset = if (config.randomness > 0.0) {
            Vector(
                (Math.random() - 0.5) * config.randomness,
                (Math.random() - 0.5) * config.randomness,
                (Math.random() - 0.5) * config.randomness
            )
        } else {
            Vector(0.0, 0.0, 0.0)
        }

        // 2. pulse: oscilar el radio en función del ángulo tridimensional
        val pulseScale = if (config.pulse) {
            (Math.sin(angle * 2.0) + 1.2) / 1.5
        } else {
            1.0
        }
        val activeRadius = radius * pulseScale

        // 3. speedScaling: escalar el spread físico de partículas según la inercia del proyectil
        val activeSpeed = if (config.speedScaling) {
            speed * (velLength / 1.5).coerceIn(0.5, 2.5)
        } else {
            speed
        }

        // 4. lifetimeFade & colorGradients
        val parsedColors = (config.gradient + config.gradients).mapNotNull { parseHexColor(it) }
        val activeColor = if (parsedColors.isNotEmpty()) {
            val factor = ((angle % (2.0 * Math.PI)) / (2.0 * Math.PI)).toFloat()
            getGradientColor(parsedColors, factor)
        } else {
            org.bukkit.Color.fromRGB(config.colorR, config.colorG, config.colorB)
        }

        val builder = ParticleBuilder(particleType)
            .count(count)
            .offset(offsetX + randomOffset.x, offsetY + randomOffset.y, offsetZ + randomOffset.z)
            .extra(activeSpeed)

        // Inspector dinámico y robusto de tipo de datos requerido por la API de Paper
        val dataType = try {
            particleType.dataType
        } catch (e: Exception) {
            Void::class.java
        }

        if (dataType != Void::class.java) {
            try {
                when (dataType) {
                    java.lang.Float::class.java -> {
                        builder.data(1.0f)
                    }
                    org.bukkit.Color::class.java -> {
                        builder.data(activeColor)
                    }
                    org.bukkit.Particle.DustOptions::class.java -> {
                        val size = if (config.lifetimeFade) {
                            config.dustSize * (1.0f - ((angle % (4.0 * Math.PI)) / (5.0 * Math.PI)).toFloat().coerceIn(0.0f, 0.5f))
                        } else {
                            config.dustSize
                        }
                        builder.data(Particle.DustOptions(activeColor, size))
                    }
                    org.bukkit.Particle.DustTransition::class.java -> {
                        val size = if (config.lifetimeFade) config.dustSize * 0.8f else config.dustSize
                        builder.data(org.bukkit.Particle.DustTransition(activeColor, org.bukkit.Color.WHITE, size))
                    }
                    org.bukkit.block.data.BlockData::class.java -> {
                        builder.data(org.bukkit.Bukkit.createBlockData(org.bukkit.Material.AMETHYST_BLOCK))
                    }
                    org.bukkit.inventory.ItemStack::class.java -> {
                        builder.data(org.bukkit.inventory.ItemStack(org.bukkit.Material.AMETHYST_SHARD))
                    }
                }
            } catch (e: Exception) {
                // Silenciosamente capturar y reaccionar si ocurre algún problema con la inyección
            }
        }

        // --- MATEMÁTICAS 3D Y EFECTOS GEOMÉTRICOS ---
        val finalLoc = when (config.style.uppercase()) {
            "LINEAR" -> {
                loc.clone().add(randomOffset)
            }
            "SPIRAL", "HELIX" -> {
                // Generar base ortonormal perpendicular al vector de velocidad del proyectil
                val u = dir
                val temp = if (Math.abs(u.x) < 0.9) Vector(1.0, 0.0, 0.0) else Vector(0.0, 1.0, 0.0)
                val v = u.getCrossProduct(temp).normalize()
                val w = u.getCrossProduct(v).normalize()

                val cos = Math.cos(angle) * activeRadius
                val sin = Math.sin(angle) * activeRadius
                val offsetVector = v.clone().multiply(cos).add(w.clone().multiply(sin))
                
                loc.clone().add(offsetVector).add(randomOffset)
            }
            "DOUBLE_HELIX" -> {
                // Generar base ortonormal perpendicular
                val u = dir
                val temp = if (Math.abs(u.x) < 0.9) Vector(1.0, 0.0, 0.0) else Vector(0.0, 1.0, 0.0)
                val v = u.getCrossProduct(temp).normalize()
                val w = u.getCrossProduct(v).normalize()

                // Hélice A
                val cosA = Math.cos(angle) * activeRadius
                val sinA = Math.sin(angle) * activeRadius
                val offsetA = v.clone().multiply(cosA).add(w.clone().multiply(sinA))
                
                builder.location(loc.clone().add(offsetA).add(randomOffset))
                builder.spawn()

                // Hélice B (Rotada 180° / PI)
                val cosB = Math.cos(angle + Math.PI) * activeRadius
                val sinB = Math.sin(angle + Math.PI) * activeRadius
                val offsetB = v.clone().multiply(cosB).add(w.clone().multiply(sinB))
                
                loc.clone().add(offsetB).add(randomOffset)
            }
            "ORBIT" -> {
                // Rotación circular en torno al proyectil
                val u = dir
                val temp = if (Math.abs(u.x) < 0.9) Vector(1.0, 0.0, 0.0) else Vector(0.0, 1.0, 0.0)
                val v = u.getCrossProduct(temp).normalize()
                val w = u.getCrossProduct(v).normalize()

                val cos = Math.cos(angle * 2.0) * activeRadius
                val sin = Math.sin(angle * 2.0) * activeRadius
                val offsetVector = v.clone().multiply(cos).add(w.clone().multiply(sin))
                
                loc.clone().add(offsetVector).add(randomOffset)
            }
            "RIBBON", "WAVE" -> {
                // Generar base ortonormal
                val u = dir
                val temp = if (Math.abs(u.x) < 0.9) Vector(1.0, 0.0, 0.0) else Vector(0.0, 1.0, 0.0)
                val v = u.getCrossProduct(temp).normalize()

                // Onda sinusoidal pura perpendicular a la trayectoria
                val waveOffset = Math.sin(angle * config.waveSpeed) * config.waveAmplitude
                val offsetVector = v.clone().multiply(waveOffset)

                loc.clone().add(offsetVector).add(randomOffset)
            }
            "CHAOS" -> {
                // Generar base ortonormal
                val u = dir
                val temp = if (Math.abs(u.x) < 0.9) Vector(1.0, 0.0, 0.0) else Vector(0.0, 1.0, 0.0)
                val v = u.getCrossProduct(temp).normalize()
                val w = u.getCrossProduct(v).normalize()

                // Desplazamiento caótico en ángulo impredecible
                val chaosScale = activeRadius * (1.0 + config.randomness * (Math.random() - 0.5))
                val randAngle = Math.random() * 2.0 * Math.PI
                val offsetVector = v.clone().multiply(Math.cos(randAngle) * chaosScale)
                    .add(w.clone().multiply(Math.sin(randAngle) * chaosScale))

                loc.clone().add(offsetVector).add(randomOffset)
            }
            "VORTEX" -> {
                // Generar base ortonormal
                val u = dir
                val temp = if (Math.abs(u.x) < 0.9) Vector(1.0, 0.0, 0.0) else Vector(0.0, 1.0, 0.0)
                val v = u.getCrossProduct(temp).normalize()
                val w = u.getCrossProduct(v).normalize()

                // Un vórtice de tres brazos espirales dinámicos
                val arms = 3
                for (arm in 0 until arms) {
                    val armOffsetAngle = arm * (2.0 * Math.PI / arms)
                    val currentRadius = activeRadius * (0.3 + 0.7 * Math.sin(angle + armOffsetAngle))
                    val cos = Math.cos(angle + armOffsetAngle) * currentRadius
                    val sin = Math.sin(angle + armOffsetAngle) * currentRadius
                    val offsetVector = v.clone().multiply(cos).add(w.clone().multiply(sin))
                    
                    val armLoc = loc.clone().add(offsetVector).add(randomOffset)
                    val armBuilder = builder.clone().location(armLoc)
                    
                    if (particleType == Particle.DUST) {
                        armBuilder.data(builder.data())
                    }
                    armBuilder.spawn()
                }
                return // Ya se renderizaron los brazos del vórtice
            }
            else -> {
                loc.clone().add(randomOffset)
            }
        }

        builder.location(finalLoc)
        builder.spawn()
    }

    // --- SISTEMA DE CONDICIONES COMPATIBLE Y SEGURO ---
    private fun checkConditions(shooter: Player?, proj: Projectile, conditions: List<String>): Boolean {
        if (conditions.isEmpty()) return true
        for (cond in conditions) {
            when (cond.uppercase()) {
                "SNEAKING" -> {
                    if (shooter == null || !shooter.isSneaking) return false
                }
                "IN_WATER" -> {
                    if (!proj.location.block.isLiquid) return false
                }
                "ON_FIRE" -> {
                    if (proj.fireTicks <= 0) return false
                }
                "GLIDING" -> {
                    if (shooter == null || !shooter.isGliding) return false
                }
            }
        }
        return true
    }

    // --- UTILIDADES DE TRATAMIENTO DE COLOR Y GRADIENTES ---
    private fun parseHexColor(hex: String): org.bukkit.Color? {
        val cleanHex = hex.replace("#", "")
        return try {
            val r = cleanHex.substring(0, 2).toInt(16)
            val g = cleanHex.substring(2, 4).toInt(16)
            val b = cleanHex.substring(4, 6).toInt(16)
            org.bukkit.Color.fromRGB(r, g, b)
        } catch (e: Exception) {
            null
        }
    }

    private fun interpolateColor(c1: org.bukkit.Color, c2: org.bukkit.Color, ratio: Float): org.bukkit.Color {
        val r = (c1.red + ratio * (c2.red - c1.red)).toInt().coerceIn(0, 255)
        val g = (c1.green + ratio * (c2.green - c1.green)).toInt().coerceIn(0, 255)
        val b = (c1.blue + ratio * (c2.blue - c1.blue)).toInt().coerceIn(0, 255)
        return org.bukkit.Color.fromRGB(r, g, b)
    }

    private fun getGradientColor(colors: List<org.bukkit.Color>, factor: Float): org.bukkit.Color {
        if (colors.isEmpty()) return org.bukkit.Color.WHITE
        if (colors.size == 1) return colors[0]

        val size = colors.size
        val scaledFactor = factor.coerceIn(0.0f, 1.0f) * (size - 1)
        val index = scaledFactor.toInt()
        val nextIndex = (index + 1).coerceAtMost(size - 1)
        val ratio = scaledFactor - index

        return interpolateColor(colors[index], colors[nextIndex], ratio)
    }
}
