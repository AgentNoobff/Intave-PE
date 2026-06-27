package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WindowBulkItemReader extends AbstractPacketReader implements WindowItemReader {
  private WrapperPlayServerWindowItems wrapper() {
    return new WrapperPlayServerWindowItems((PacketSendEvent) event());
  }

  @Override
  public int windowId() {
    return wrapper().getWindowId();
  }

  @Override
  public Map<Integer, ItemStack> itemMap() {
    List<com.github.retrooper.packetevents.protocol.item.ItemStack> items = wrapper().getItems();
    Map<Integer, ItemStack> map = new HashMap<>();
    if (items == null) {
      return map;
    }
    for (int i = 0; i < items.size(); i++) {
      map.put(i, SpigotConversionUtil.toBukkitItemStack(items.get(i)));
    }
    return map;
  }
}
