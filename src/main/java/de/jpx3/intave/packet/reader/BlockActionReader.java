package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockAction;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Material;

public final class BlockActionReader extends AbstractPacketReader {
  private WrapperPlayServerBlockAction wrapper() {
    return new WrapperPlayServerBlockAction((PacketSendEvent) event());
  }

  public Vector3i blockPosition() {
    return wrapper().getBlockPosition();
  }

  public Material blockType() {
    try {
      return SpigotConversionUtil.toBukkitBlockData(wrapper().getBlockType()).getMaterial();
    } catch (Throwable throwable) {
      // TODO(pe-migration): legacy (pre-1.13) servers have no Bukkit BlockData; resolve the block
      // type via material-data if a caller relies on this below 1.13.
      return Material.AIR;
    }
  }

  public int action() {
    return wrapper().getActionId();
  }

  public int data() {
    return wrapper().getActionData();
  }
}
