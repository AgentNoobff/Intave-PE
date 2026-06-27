package de.jpx3.intave.player.fake.action;

import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.player.fake.FakePlayer;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public final class EquipmentHeldItemAction extends Action {
  public EquipmentHeldItemAction(Player player, FakePlayer fakePlayer) {
    super(Probability.MEDIUM, player, fakePlayer);
  }

  @Override
  public void perform() {
    de.jpx3.intave.player.fake.equipment.Equipment equipment =
      de.jpx3.intave.player.fake.equipment.EquipmentFactory.randomEquipment();
    Material heldItem = equipment.heldItem();
    if (heldItem != Material.AIR) {
      updateHeldItem(heldItem);
    }
  }

  private static final boolean HAS_OFF_HAND = MinecraftVersions.VER1_9_0.atOrAbove();

  private void updateHeldItem(Material material) {
    ItemStack itemStack = new ItemStack(material);
    EquipmentSlot hand = HAS_OFF_HAND && ThreadLocalRandom.current().nextInt(0, 10) == 5
      ? EquipmentSlot.OFF_HAND
      : EquipmentSlot.MAIN_HAND;
    Equipment equipment = new Equipment(
      hand,
      SpigotConversionUtil.fromBukkitItemStack(itemStack)
    );
    send(new WrapperPlayServerEntityEquipment(
      this.fakePlayer.identifier(),
      Collections.singletonList(equipment)
    ));
  }
}
