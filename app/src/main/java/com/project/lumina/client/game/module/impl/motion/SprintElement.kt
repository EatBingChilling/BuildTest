package com.project.lumina.client.game.module.impl.motion

import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.util.AssetManager
import com.project.lumina.client.constructors.Value
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket

class SprintElement(
    iconResId: Int = AssetManager.getAsset("ic_run_fast_black_24dp")
) : Element(
    name = "Sprint",
    category = CheatCategory.Motion,
    iconResId,
    displayNameResId = AssetManager.getString("module_sprint_display_name")
) {

    /* 通过 values 列表添加两个布尔配置项 */
    private val onlyWhenMoving: Value<Boolean> = Value("OnlyWhenMoving", true).also { values += it }
    private val keepSprinting:  Value<Boolean> = Value("KeepSprinting",  true).also { values += it }

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket) return

        when {
            isEnabled -> {
                val shouldSprint = if (onlyWhenMoving.value) isActuallyMoving(packet) else true
                if (shouldSprint) {
                    packet.inputData.add(PlayerAuthInputData.SPRINTING)
                    if (keepSprinting.value) {
                        packet.inputData.add(PlayerAuthInputData.START_SPRINTING)
                    }
                }
            }
            else -> {
                packet.inputData.add(PlayerAuthInputData.STOP_SPRINTING)
            }
        }
    }

    private fun isActuallyMoving(p: PlayerAuthInputPacket): Boolean {
        val hasMoveInput = p.inputData.any {
            it in setOf(
                PlayerAuthInputData.UP,
                PlayerAuthInputData.DOWN,
                PlayerAuthInputData.LEFT,
                PlayerAuthInputData.RIGHT
            )
        }
        val hasVelocity = p.motion.length() > 0f
        return hasMoveInput || hasVelocity
    }
}
