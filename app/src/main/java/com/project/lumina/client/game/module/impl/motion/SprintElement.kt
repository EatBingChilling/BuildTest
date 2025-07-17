package com.project.lumina.client.game.module.impl.motion

import com.project.lumina.client.R
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.util.AssetManager
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

    /* =====================  配置开关  ===================== */
    private val onlyWhenMoving by boolean("OnlyWhenMoving", true)   // 只在移动时生效
    private val keepSprinting  by boolean("KeepSprinting",  true)   // 额外加 START_SPRINTING

    /* ====================================================== */

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        val packet = interceptablePacket.packet
        if (packet !is PlayerAuthInputPacket) return

        when {
            /* 模块开启：按条件注入冲刺标志 */
            isEnabled -> {
                val shouldSprint = if (onlyWhenMoving) isActuallyMoving(packet) else true
                if (shouldSprint) {
                    packet.inputData.add(PlayerAuthInputData.SPRINTING)
                    if (keepSprinting) {
                        packet.inputData.add(PlayerAuthInputData.START_SPRINTING)
                    }
                }
            }

            /* 模块关闭：告诉游戏停止冲刺 */
            else -> {
                packet.inputData.add(PlayerAuthInputData.STOP_SPRINTING)
            }
        }
    }

    /* 判断玩家是否在移动（抄自 AutoSprintModule） */
    private fun isActuallyMoving(p: PlayerAuthInputPacket): Boolean {
        // 方向键
        val hasMoveInput = p.inputData.any {
            it in setOf(
                PlayerAuthInputData.UP,
                PlayerAuthInputData.DOWN,
                PlayerAuthInputData.LEFT,
                PlayerAuthInputData.RIGHT
            )
        }
        // 速度向量
        val hasVelocity = p.motion.length() > 0f
        return hasMoveInput || hasVelocity
    }
}
