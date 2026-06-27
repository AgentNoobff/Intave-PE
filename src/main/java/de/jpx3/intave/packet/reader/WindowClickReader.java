package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserRepository;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class WindowClickReader extends AbstractPacketReader {

  private WrapperPlayClientClickWindow wrapper() {
    return new WrapperPlayClientClickWindow((PacketReceiveEvent) event());
  }

  public InventoryClickType clickType() {
    WrapperPlayClientClickWindow.WindowClickType type = wrapper().getWindowClickType();
    try {
      return InventoryClickType.valueOf(type.name());
    } catch (IllegalArgumentException unknown) {
      return InventoryClickType.PICKUP;
    }
  }

  public int container() {
    return wrapper().getWindowId();
  }

  public String clickedItemTypeIfPossible(Player player) {
    if (container() == 0 && slot() >= 0) {
      User user = UserRepository.userOf(player);
      List<String> items = user.meta().inventory().items();
      int slot = slot();
      return items == null || slot >= items.size() ? null : items.get(slot);
    } else {
      return null;
    }
  }

  public int slot() {
    return wrapper().getSlot();
  }

  public int button() {
    return wrapper().getButton();
  }

  public int actionNumber() {
    return wrapper().getActionNumber().orElse(0);
  }

  public ItemStack itemStack() {
    return SpigotConversionUtil.toBukkitItemStack(wrapper().getCarriedItemStack());
  }

  public boolean isDrop() {
    return clickType() == InventoryClickType.THROW && slot() != -999;
  }

  public boolean missingItemStack() {
    switch (clickType()) {
      case QUICK_MOVE:
      case SWAP:
        return true;
      default:
        return false;
    }
  }

  public enum InventoryClickType {
    PICKUP,
    QUICK_MOVE,
    SWAP,
    CLONE,
    THROW,
    QUICK_CRAFT,
    PICKUP_ALL
  }
}
