package de.jpx3.intave.player.fake;

import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.CollisionRule;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.NameTagVisibility;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.OptionData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.ScoreBoardTeamInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.TeamMode;
import de.jpx3.intave.packet.PacketSender;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Collections;

public final class ScoreboardAccessor {
  public static void sendScoreboard(
    Player player,
    String teamName,
    UserProfile fakePlayerProfile,
    boolean hideNameTag
  ) {
    NameTagVisibility visibility = hideNameTag ? NameTagVisibility.NEVER : NameTagVisibility.ALWAYS;
    ScoreBoardTeamInfo teamInfo = new ScoreBoardTeamInfo(
      Component.text(teamName),
      Component.empty(),
      Component.empty(),
      visibility,
      CollisionRule.ALWAYS,
      null,
      OptionData.NONE
    );
    WrapperPlayServerTeams packet = new WrapperPlayServerTeams(
      teamName,
      TeamMode.CREATE,
      teamInfo,
      Collections.singletonList(fakePlayerProfile.getName())
    );
    PacketSender.sendServerPacket(player, packet);
  }
}
