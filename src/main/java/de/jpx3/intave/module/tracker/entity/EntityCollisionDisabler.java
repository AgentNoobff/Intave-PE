package de.jpx3.intave.module.tracker.entity;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.module.Module;
import de.jpx3.intave.module.linker.packet.ListenerPriority;
import de.jpx3.intave.module.linker.packet.PacketSubscription;

import java.util.Optional;

import static de.jpx3.intave.module.linker.packet.PacketId.Server.SCOREBOARD_TEAM;

public final class EntityCollisionDisabler extends Module {
  private static final boolean DISABLE_ENTITY_COLLISIONS = MinecraftVersions.VER1_9_0.atOrAbove();

  @PacketSubscription(
    priority = ListenerPriority.HIGHEST,
    packetsOut = {
      SCOREBOARD_TEAM
    }
  )
  public void receiveScoreboardUpdate(ProtocolPacketEvent event) {
    if (!DISABLE_ENTITY_COLLISIONS) {
      return;
    }
    // PacketEvents normalises the team-info structure across versions, so the version-specific field
    // index arithmetic the ProtocolLib code carried is no longer required.
    WrapperPlayServerTeams packet = new WrapperPlayServerTeams((PacketSendEvent) event);
    Optional<WrapperPlayServerTeams.ScoreBoardTeamInfo> teamInfo = packet.getTeamInfo();
    if (teamInfo.isPresent()) {
      WrapperPlayServerTeams.ScoreBoardTeamInfo info = teamInfo.get();
      info.setCollisionRule(WrapperPlayServerTeams.CollisionRule.NEVER);
      packet.setTeamInfo(info);
      event.markForReEncode(true);
    }
  }
}
