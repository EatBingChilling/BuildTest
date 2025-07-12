package com.project.lumina.client.game.module.impl.combat

import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.constructors.GameManager
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.game.entity.Player
import com.project.lumina.client.game.entity.LocalPlayer
import com.project.lumina.client.util.AssetManager
import com.project.lumina.client.game.InterceptablePacket

class AntiBotElement(iconResId: Int = AssetManager.getAsset("ic_ghost_black_24dp")) : Element(
    name = "AntiBot",
    category = CheatCategory.Combat,
    iconResId,
    displayNameResId = AssetManager.getString("module_antibot_display_name")
) {

    private var antiBotModeValue by intValue("模式", 0, 0..1)

    fun Player.isBot(): Boolean {
        if (this is LocalPlayer) return false

        return when (antiBotModeValue) {
            0 -> false
            1 -> {
                val currentPlayers = GameManager.netBound?.getCurrentPlayers() ?: emptyList()
                val playerInfo = currentPlayers.find { it.uuid == this.uuid }
                playerInfo == null || playerInfo.name.isBlank()
            }
            else -> false
        }
    }
    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        
    }
}