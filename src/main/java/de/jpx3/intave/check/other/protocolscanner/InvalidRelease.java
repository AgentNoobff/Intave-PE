package de.jpx3.intave.check.other.protocolscanner;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import de.jpx3.intave.check.CheckPart;
import de.jpx3.intave.check.other.ProtocolScanner;
import de.jpx3.intave.module.Modules;
import de.jpx3.intave.module.linker.packet.PacketId;
import de.jpx3.intave.module.linker.packet.PacketSubscription;
import de.jpx3.intave.module.violation.Violation;
import de.jpx3.intave.user.User;
import org.bukkit.entity.Player;

public class InvalidRelease extends CheckPart<ProtocolScanner> {

    public InvalidRelease(ProtocolScanner parentCheck) {
        super(parentCheck);
    }

    @PacketSubscription(packetsIn = {
            PacketId.Client.BLOCK_DIG
    })
    public void checkValidateRelease(ProtocolPacketEvent event) {
        WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging((PacketReceiveEvent) event);
        Player player = event.getPlayer();
        User user = userOf(player);
        DiggingAction digType = packet.getAction();
        if (digType == null) return;
        if (user.protocolVersion() < 47) return;
        if (digType == DiggingAction.RELEASE_USE_ITEM) {
            BlockFace face = packet.getBlockFace();
            // Vanilla always sends DOWN
            // Fix https://github.com/Raven-APlus/RavenAPlus/blob/master/src/main/java/keystrokesmod/module/impl/movement/noslow/IntaveNoSlow.java
            if (face != BlockFace.DOWN) {
                Violation violation = Violation.builderFor(ProtocolScanner.class)
                        .forPlayer(player).withMessage("sent invalid release").withDetails("face " + face)
                        .withVL(3)
                        .build();
                Modules.violationProcessor().processViolation(violation);
            }
        }
    }
}
