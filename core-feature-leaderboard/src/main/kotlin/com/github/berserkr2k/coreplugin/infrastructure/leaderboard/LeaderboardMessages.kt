package com.github.berserkr2k.coreplugin.infrastructure.leaderboard

import com.github.berserkr2k.coreplugin.api.core.message.MessageKey

enum class LeaderboardMessages(override val path: String) : MessageKey {
    ONLY_PLAYERS("only-players"),
    REGISTERED("registered"),
    CREATED("created"),
    DELETED("deleted"),
    NOT_FOUND("not-found"),
    RELOADED("reloaded"),
    VARA_RECIBIDA("vara-recibida"),
    COPIED("copied"),
    NO_CLIPBOARD("no-clipboard"),
    PASTED_ALL("pasted-all"),
    PASTED_POSE("pasted-pose"),
    WRITE_NAME("write-name"),
    SCALE_CHANGED("scale-changed"),
    EQUIP_SYNCED("equip-synced"),
    POSE_TOOL("pose-tool"),
    LEADERBOARD_LOADING("loading"),
    LEADERBOARD_VACANT("vacant"),
    LEADERBOARD_ARMORSTAND_NOT_FOUND("armorstand-not-found"),
    LEADERBOARD_NAME_ASSIGNED("name-assigned"),
    LEADERBOARD_POSE_ACTIONBAR("pose-actionbar"),
    SCALE_COARSE_NAME("scale-coarse-name"),
    SCALE_FINE_NAME("scale-fine-name"),
    GUI_SCALE_COARSE("gui.scale-coarse"),
    GUI_SCALE_FINE("gui.scale-fine"),
    GUI_STATUS_YES("gui.status-yes"),
    GUI_STATUS_NO("gui.status-no"),
    PART_HEAD("part.head"),
    PART_BODY("part.body"),
    PART_LEFT_ARM("part.left-arm"),
    PART_RIGHT_ARM("part.right-arm"),
    PART_LEFT_LEG("part.left-leg"),
    PART_RIGHT_LEG("part.right-leg");

    override val feature: String = "leaderboard"

    companion object {
        val defaults = mapOf(
            "only-players" to "<red>Solo jugadores pueden ejecutar este comando.</red>",
            "registered" to "<green>¡ArmorStand registrado con éxito como podio para '<id>' (Top <rank>)!</green>",
            "created" to "<green>¡Nuevo ArmorStand de podio creado para '<id>' (Top <rank>)!</green>",
            "deleted" to "<green>¡Podio para '<id>' (Top <rank>) eliminado con éxito!</green>",
            "not-found" to "<red>No se encontró ningún podio para la clasificación '<id>' con Rank <rank>.</red>",
            "reloaded" to "<green>✔ ¡Configuraciones de clasificaciones recargadas y actualizadas con éxito!</green>",
            "vara-recibida" to "<green>¡Has recibido la Vara de Edición!</green>",
            "copied" to "<green>✔ ¡Propiedades físicas y de pose copiadas al portapapeles!</green>",
            "no-clipboard" to "<red>❌ No tienes ajustes copiados en el portapapeles.</red>",
            "pasted-all" to "<green>✔ ¡Ajustes y equipamiento pegados con éxito!</green>",
            "pasted-pose" to "<green>✔ ¡Ajustes de pose aplicados! (Los objetos no se duplicaron por seguridad en Supervivencia)</green>",
            "write-name" to "<gold>✏ Escribe el nombre del ArmorStand en el chat (Soporta colores con &):</gold>",
            "scale-changed" to "<green>✔ Escala de ajuste cambiada a <scale>.</green>",
            "equip-synced" to "<green>✔ ¡El equipamiento del ArmorStand se ha sincronizado exitosamente!</green>",
            "pose-tool" to "<green>✔ ¡Recibiste la <gold>Herramienta de Pose</gold>! Ajusta libremente. Sneak + Click Derecho para volver.</green>",
            "loading" to "<gold>Cargando datos del podio...</gold>",
            "vacant" to "<gray>#<pos> - Vacante</gray>",
            "armorstand-not-found" to "<red>El ArmorStand ya no existe.</red>",
            "name-assigned" to "<green>¡Nombre asignado con éxito!</green>",
            "pose-actionbar" to "<gold><bold><part> (<axis>): <angle>°</bold></gold> <gray>(Modo: <mode>)</gray>",
            "scale-coarse-name" to "GRUESO",
            "scale-fine-name" to "FINO",
            "gui.scale-coarse" to "<yellow>Modo actual: <bold>GRUESO</bold></yellow> (<gray>15° / 0.5m</gray>)",
            "gui.scale-fine" to "<yellow>Modo actual: <bold>FINO</bold></yellow> (<gray>1° / 0.05m</gray>)",
            "gui.status-yes" to "<green>✔ SÍ</green>",
            "gui.status-no" to "<red>❌ NO</red>",
            "part.head" to "Cabeza",
            "part.body" to "Cuerpo",
            "part.left-arm" to "Brazo Izquierdo",
            "part.right-arm" to "Brazo Derecho",
            "part.left-leg" to "Pierna Izquierda",
            "part.right-leg" to "Pierna Derecha"
        )
    }
}
