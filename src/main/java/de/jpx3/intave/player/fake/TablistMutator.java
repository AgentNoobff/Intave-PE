package de.jpx3.intave.player.fake;

import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.packet.PacketSender;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;

public final class TablistMutator {
  private static final boolean MODERN_PLAYER_INFO = MinecraftVersions.VER1_19_3.atOrAbove();

  public static void addToTabList(
    Player player,
    UserProfile wrappedGameProfile,
    String tabListName
  ) {
    addToTabList(player, wrappedGameProfile, Component.text(tabListName));
  }

  private static void addToTabList(
    Player player,
    UserProfile profile,
    Component displayName
  ) {
    int latency = ThreadLocalRandom.current().nextInt(20, 200);
    PacketWrapper<?> packet;
    if (MODERN_PLAYER_INFO) {
      WrapperPlayServerPlayerInfoUpdate.PlayerInfo info = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
        profile, true, latency, GameMode.SURVIVAL, displayName, null
      );
      packet = new WrapperPlayServerPlayerInfoUpdate(
        EnumSet.of(
          WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
          WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED,
          WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY,
          WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE,
          WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME
        ),
        info
      );
    } else {
      WrapperPlayServerPlayerInfo.PlayerData playerData = new WrapperPlayServerPlayerInfo.PlayerData(
        displayName, profile, GameMode.SURVIVAL, latency
      );
      packet = new WrapperPlayServerPlayerInfo(
        WrapperPlayServerPlayerInfo.Action.ADD_PLAYER, playerData
      );
    }
    PacketSender.sendServerPacket(player, packet);
  }

  public static void removeFromTabList(
    Player player,
    UserProfile profile
  ) {
    PacketWrapper<?> packet;
    if (MODERN_PLAYER_INFO) {
      packet = new WrapperPlayServerPlayerInfoRemove(
        Collections.singletonList(profile.getUUID())
      );
    } else {
      WrapperPlayServerPlayerInfo.PlayerData playerData = new WrapperPlayServerPlayerInfo.PlayerData(
        Component.text(profile.getName()), profile, GameMode.SURVIVAL,
        ThreadLocalRandom.current().nextInt(20, 200)
      );
      packet = new WrapperPlayServerPlayerInfo(
        WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER, playerData
      );
    }
    PacketSender.sendServerPacket(player, packet);
  }
}
