package de.jpx3.intave.module.tracker.player;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerHeldItemChange;
import de.jpx3.intave.IntaveControl;
import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.module.Module;
import de.jpx3.intave.module.Modules;
import de.jpx3.intave.module.linker.bukkit.BukkitEventSubscription;
import de.jpx3.intave.module.linker.packet.ListenerPriority;
import de.jpx3.intave.module.linker.packet.PacketSubscription;
import de.jpx3.intave.packet.PacketSender;
import de.jpx3.intave.player.ItemProperties;
import de.jpx3.intave.user.MessageChannel;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserRepository;
import de.jpx3.intave.user.meta.InventoryMetadata;
import de.jpx3.intave.user.meta.PunishmentMetadata;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import static de.jpx3.intave.module.linker.packet.PacketId.Client.*;
import static de.jpx3.intave.module.linker.packet.PacketId.Server.HELD_ITEM_SLOT_OUT;

public class PlayerHandTracker extends Module {
  private final boolean NEW_ITEM_REQUEST = MinecraftVersions.VER1_9_0.atOrAbove();

//  @BukkitEventSubscription
//  public void itemConsume(FoodLevelChangeEvent event) {
//    if (!(event.getEntity() instanceof Player)) {
//      return;
//    }
//    Player player = (Player) event.getEntity();
//    User user = UserRepository.userOf(player);
//    InventoryMetadata inventoryData = user.meta().inventory();
//    if (event.getFoodLevel() >= 20 && inventoryData.foodItem() && inventoryData.handActive()) {
//      inventoryData.deactivateHand();
//    }
//  }

  @BukkitEventSubscription
  public void entityFoodChange(FoodLevelChangeEvent event) {
    HumanEntity entity = event.getEntity();
    if (!(entity instanceof Player)) {
      return;
    }

    Player player = (Player) entity;
    User user = UserRepository.userOf(player);
    InventoryMetadata inventoryData = user.meta().inventory();
    int foodLevel = event.getFoodLevel();

    if (foodLevel >= 20 && inventoryData.handActive() && inventoryData.foodItem()) {
      if (!ItemProperties.foodConsumable(player, inventoryData.heldItemType())) {
        inventoryData.deactivateHand();
      }
    }
  }

  @BukkitEventSubscription
  public void receiveItemConsume(PlayerItemConsumeEvent event) {
    Player player = event.getPlayer();
    User user = UserRepository.userOf(player);
    InventoryMetadata inventoryData = user.meta().inventory();
    inventoryData.deactivateHand();
  }

  @PacketSubscription(
    priority = ListenerPriority.LOWEST,
    packetsIn = {
      HELD_ITEM_SLOT_IN
    }
  )
  public void receiveSlotSwitch(ProtocolPacketEvent event) {
    Player player = event.getPlayer();

    User user = UserRepository.userOf(player);
    InventoryMetadata inventoryData = user.meta().inventory();

    int slot = new WrapperPlayClientHeldItemChange((PacketReceiveEvent) event).getSlot();

    if (isInvalidSlot(slot)) {
      return;
    }

//    if (IntaveControl.DEBUG_ITEM_USAGE) {
//      ItemStack item = player.getInventory().getItem(slot);
//      String typeName = item == null ? "AIR" : item.getType().name();
////      Synchronizer.synchronize(() -> {
//        player.sendMessage("(async) Slot changed to " + slot + ", type: " + typeName);
////      });
//    }

    // apparently required?
    inventoryData.setHeldItemSlot(slot);

    ItemStack item = player.getInventory().getItem(slot);
    inventoryData.pastSlotSwitch = 0;
    if (inventoryData.handActive() && !inventoryData.offhandItemPrimary()) {
      inventoryData.releaseItemNextTick();
      ItemStack itemStack = inventoryData.heldItem();
      if (!ItemProperties.canItemBeUsed(player, itemStack)) {
        inventoryData.blockNextArrow = true;
        inventoryData.lastBlockArrowRequest = System.currentTimeMillis();
        if (user.receives(MessageChannel.DEBUG_ITEM_RESETS)) {
          user.player().sendMessage(IntavePlugin.prefix() + " Detected item switch on active item, released hand and blocking impending arrow shot");
        }
      }
    }
    inventoryData.slotSwitchData = new InventoryMetadata.SlotSwitchData(slot, item);
  }

  @PacketSubscription(
    packetsOut = {
      HELD_ITEM_SLOT_OUT
    }
  )
  public void sentSlotSwitch(ProtocolPacketEvent event) {
    Player player = event.getPlayer();
    User user = UserRepository.userOf(player);
    int slot = new WrapperPlayServerHeldItemChange((PacketSendEvent) event).getSlot();

    if (isInvalidSlot(slot)) {
      return;
    }

    Modules.feedback().synchronize(player, slot, (player1, slot1) -> {
      user.meta().inventory().setHeldItemSlot(slot);
    });
  }

  private boolean isInvalidSlot(int slot) {
    return slot >= 36 || slot < 0;
  }

  @PacketSubscription(
    priority = ListenerPriority.LOWEST,
    packetsIn = {
      BLOCK_PLACE, USE_ITEM, USE_ITEM_ON
    }
  )
  public void receiveBlockPlace(ProtocolPacketEvent event) {
    Player player = event.getPlayer();
    User user = UserRepository.userOf(player);
    boolean requestedItemUse = requestedItemUseLegacy(event);

    if (requestedItemUse) {
      handleItemUseRequest(event, user);
    }
  }

  private boolean requestedItemUseLegacy(ProtocolPacketEvent event) {
    if (NEW_ITEM_REQUEST) {
      return true;
    } else {
      // Pre-1.9 item-use-in-air encodes a face id of 255 on the block placement packet.
      return new WrapperPlayClientPlayerBlockPlacement((PacketReceiveEvent) event).getFaceId() == 255;
    }
  }

  private void handleItemUseRequest(ProtocolPacketEvent event, User user) {
    InventoryMetadata inventoryData = user.meta().inventory();
    PunishmentMetadata punishmentData = user.meta().punishment();

    ItemStack heldItem = inventoryData.heldItem();
    ItemStack offhandItem = inventoryData.offhandItem();

    boolean sword = heldItem != null && heldItem.getType().name().endsWith("_SWORD");

    if (sword && System.currentTimeMillis() - punishmentData.timeLastBlockCancel < 5000) {
      event.setCancelled(true);
      return;
    }

    boolean offHandUsable = ItemProperties.canItemBeUsed(user.player(), offhandItem);
    boolean mainHandUsable = ItemProperties.canItemBeUsed(user.player(), heldItem);
    boolean useItem = mainHandUsable || offHandUsable;

    // For some reason Minecraft sends BlockPlace packets on 1.9+ with diamond swords
    boolean usingSword = mainHandUsable && sword;
    if (usingSword && !offHandUsable && user.protocolVersion() > 47) {
      return;
    }

    if (useItem) {
      inventoryData.activateHand();
    }
  }

  @PacketSubscription(
    priority = ListenerPriority.HIGH,
    packetsIn = {
      BLOCK_DIG
    }
  )
  public void receiveBlockDigging(ProtocolPacketEvent event) {
    Player player = event.getPlayer();
    User user = UserRepository.userOf(player);
    InventoryMetadata inventoryData = user.meta().inventory();

    WrapperPlayClientPlayerDigging packet =
      new WrapperPlayClientPlayerDigging((PacketReceiveEvent) event);
    DiggingAction digType = packet.getAction();

    Vector3i blockPosition = packet.getBlockPosition();
    boolean atOrigin = blockPosition != null
      && blockPosition.getX() == 0 && blockPosition.getY() == 0 && blockPosition.getZ() == 0;

    if (digType == DiggingAction.RELEASE_USE_ITEM
      && !inventoryData.handActive()
      && packet.getBlockFace() == BlockFace.DOWN
      && atOrigin
    ) {
      return;
    }

    if (IntaveControl.DEBUG_ITEM_USAGE) {
      player.sendMessage("Digtype: " + digType);
    }

    switch (digType) {
      case RELEASE_USE_ITEM:
      case DROP_ITEM_STACK:
      case DROP_ITEM: {
        inventoryData.deactivateHand();
        break;
      }
    }

    boolean usedFoodItem = inventoryData.foodItem() && inventoryData.handActive();
    // Fix eating while sprinting bug: https://www.youtube.com/watch?v=5ZHMrVmtdNY
    if (digType == DiggingAction.DROP_ITEM && usedFoodItem) {
      WrapperPlayClientPlayerDigging unblockPacket = new WrapperPlayClientPlayerDigging(
        DiggingAction.RELEASE_USE_ITEM, blockPosition, packet.getBlockFace(), packet.getBlockFaceId()
      );
      user.ignoreNextInboundPacket();
      PacketSender.receiveClientPacketFrom(player, unblockPacket);
    }
  }
}
