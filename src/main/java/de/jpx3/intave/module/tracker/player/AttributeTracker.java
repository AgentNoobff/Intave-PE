package de.jpx3.intave.module.tracker.player;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes.Property;
import de.jpx3.intave.module.Module;
import de.jpx3.intave.module.linker.packet.ListenerPriority;
import de.jpx3.intave.module.linker.packet.PacketSubscription;
import de.jpx3.intave.player.attribute.Attribute;
import de.jpx3.intave.player.attribute.AttributeModifier;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserRepository;
import de.jpx3.intave.user.meta.AbilityMetadata;
import de.jpx3.intave.user.meta.MovementMetadata;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static de.jpx3.intave.module.linker.packet.PacketId.Server.UPDATE_ATTRIBUTES;

public final class AttributeTracker extends Module {
  @PacketSubscription(
    priority = ListenerPriority.HIGH,
    packetsOut = {
      UPDATE_ATTRIBUTES
    }
  )
  public void sentAttributes(ProtocolPacketEvent event) {
    Player player = event.getPlayer();
    User user = UserRepository.userOf(player);
    WrapperPlayServerUpdateAttributes packet =
      new WrapperPlayServerUpdateAttributes((PacketSendEvent) event);
    if (packet.getEntityId() == player.getEntityId()) {
      List<Property> attributes = packet.getProperties();
      user.tickFeedback(() -> {
        attributes.forEach(attribute -> receivedAttribute(user, attribute));
      });
    }
  }

  private void receivedAttribute(User user, Property attribute) {
    AbilityMetadata abilities = user.meta().abilities();
    MovementMetadata movement = user.meta().movement();
    String attributeKey = attribute.getKey();
    if (abilities.findAttribute(attributeKey) != null) {
      Attribute intaveAttribute = Attribute.fromProtocolLib(attribute);
      List<AttributeModifier> intaveAttributes = abilities.modifiersOf(intaveAttribute);
      intaveAttributes.clear();
      Set<AttributeModifier> serverAttributes = intaveAttribute.modifiers();
      movement.hasSprintSpeed = serverAttributes.contains(MovementMetadata.SPRINTING_MODIFIER);
      intaveAttributes.addAll(new HashSet<>(serverAttributes));
      abilities.modifyBaseValue(attributeKey, attribute.getValue());
    }
  }
}
