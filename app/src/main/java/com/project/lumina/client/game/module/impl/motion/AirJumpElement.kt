package com.project.lumina.client.game.module.impl.motion

import com.project.lumina.client.R
import com.project.lumina.client.game.InterceptablePacket
import com.project.lumina.client.constructors.Element
import com.project.lumina.client.constructors.CheatCategory
import com.project.lumina.client.util.AssetManager
import org.cloudburstmc.math.vector.Vector3f
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket

class AirJumpElement(iconResId: Int = AssetManager.getAsset("ic_cloud_upload_black_24dp")) : Element(
    name = "AirJump",
    category = CheatCategory.Motion,
    iconResId,
    displayNameResId = AssetManager.getString("module_air_jump_display_name")
) {
    private var jumpValue by floatValue("Jump",0.42f,0.1f..3f)
    private var speedMultiplierValue by floatValue("SpeedMultiplier", 1f, 0.5f..3f)
    private var speedBoostValue by boolValue("Speed Boost", false)

    override fun beforePacketBound(interceptablePacket: InterceptablePacket) {
        if (!isEnabled) {
            return
        }

        val packet = interceptablePacket.packet

        if (packet is PlayerAuthInputPacket) {
            if (packet.inputData.contains(PlayerAuthInputData.JUMP_DOWN)) {
                val player = session.localPlayer
                if (!player.isOnGround ) {
                    val motionPacket = SetEntityMotionPacket().apply {
                        runtimeEntityId = player.runtimeEntityId
                        motion = if (speedBoostValue) {
                            Vector3f.from(
                                player.motionX * speedMultiplierValue,
                                jumpValue,
                                player.motionZ * speedMultiplierValue
                            )
                        } else {
                            Vector3f.from(
                                player.motionX,
                                jumpValue,
                                player.motionZ
                            )
                        }
                    }
                    session.clientBound(motionPacket)
                }
            }
        }
    }
}