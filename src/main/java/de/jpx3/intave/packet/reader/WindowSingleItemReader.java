package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class WindowSingleItemReader extends AbstractPacketReader implements WindowItemReader {
  private WrapperPlayServerSetSlot wrapper() {
    return new WrapperPlayServerSetSlot((PacketSendEvent) event());
  }

  @Override
  public int windowId() {
    return wrapper().getWindowId();
  }

  @Override
  public Map<Integer, ItemStack> itemMap() {
    WrapperPlayServerSetSlot wrapper = wrapper();
    Map<Integer, ItemStack> map = new HashMap<>();
    map.put(wrapper.getSlot(), SpigotConversionUtil.toBukkitItemStack(wrapper.getItem()));
    return map;
  }
}
