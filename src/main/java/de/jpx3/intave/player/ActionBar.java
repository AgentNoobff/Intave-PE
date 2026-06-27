package de.jpx3.intave.player;

import com.github.retrooper.packetevents.protocol.chat.ChatTypes;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessageLegacy;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.packet.PacketSender;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public final class ActionBar {
  private static final boolean DEDICATED_ACTION_BAR_PACKET = MinecraftVersions.VER1_17_0.atOrAbove();

  public static void sendActionBar(Player player, String message) {
    Component component = Component.text(message);
    PacketWrapper<?> packet;
    if (DEDICATED_ACTION_BAR_PACKET) {
      // overlay == true makes the system chat message render as an action bar
      packet = new WrapperPlayServerSystemChatMessage(true, component);
    } else {
      // GAME_INFO chat type renders in the action bar slot on legacy clients
      packet = new WrapperPlayServerChatMessage(new ChatMessageLegacy(component, ChatTypes.GAME_INFO));
    }
    PacketSender.sendServerPacketWithoutEvent(player, packet);
  }
}
