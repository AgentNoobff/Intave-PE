package de.jpx3.intave.module.linker.packet.tinyprotocol;

import de.jpx3.intave.IntavePlugin;
import io.netty.channel.Channel;
import org.bukkit.entity.Player;

/**
 * Netty pipeline injector kept from the ProtocolLib era.
 *
 * <p>Historically this resolved the ProtocolLib {@code PacketType} from a raw NMS packet and
 * dispatched a synthesized {@code PacketEvent} to the async-internal {@link
 * de.jpx3.intave.module.linker.packet.FilteringPacketAdapter}s. PacketEvents builds its events from
 * the channel's {@code ByteBuf} inside its own pipeline handler and offers no supported way to wrap
 * an arbitrary NMS packet object, so that bridge no longer exists.
 *
 * <p>To preserve behaviour, {@code Engine.ASYNC_INTERNAL} subscriptions are now routed through the
 * regular PacketEvents listener path (which already executes asynchronously on the netty threads) by
 * {@link de.jpx3.intave.module.linker.packet.PacketSubscriptionLinker}. This injector is retained
 * (per the migration decision to keep TinyProtocol) for its player-channel injection lifecycle, but
 * the outbound hook is a transparent passthrough.
 */
final class EventTinyProtocol extends TinyProtocol {
  private final InjectionService injectionService;

  public EventTinyProtocol(IntavePlugin plugin, InjectionService injectionService) {
    super(plugin);
    this.injectionService = injectionService;
  }

  @Override
  public Object onPacketOutAsync(Player receiver, Channel channel, Object packet) {
    // Dispatch is handled by PacketEvents listeners; pass the packet through untouched.
    return super.onPacketOutAsync(receiver, channel, packet);
  }
}
