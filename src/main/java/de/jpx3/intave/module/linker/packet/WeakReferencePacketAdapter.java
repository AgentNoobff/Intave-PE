package de.jpx3.intave.module.linker.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Base for Intave's PacketEvents listeners.
 *
 * <p>Unlike ProtocolLib, PacketEvents delivers every packet to every registered listener, so the
 * set of types a listener cares about is filtered in {@link #handles(PacketTypeCommon)} rather than
 * at registration time. The {@link PacketListenerCommon} handle returned by the event manager when
 * this adapter is registered is stored here so it can be unregistered later.
 */
public abstract class WeakReferencePacketAdapter extends PacketListenerAbstract {
  protected Plugin plugin;
  private final Set<PacketTypeCommon> handledTypes;
  private PacketListenerCommon handle;

  public WeakReferencePacketAdapter(Plugin plugin, PacketListenerPriority priority, PacketTypeCommon[] types) {
    super(priority);
    this.plugin = plugin;
    this.handledTypes = new HashSet<>(Arrays.asList(types));
  }

  protected final boolean handles(PacketTypeCommon type) {
    return handledTypes.contains(type);
  }

  public final Set<PacketTypeCommon> handledTypes() {
    return handledTypes;
  }

  public final void setRegistrationHandle(PacketListenerCommon handle) {
    this.handle = handle;
  }

  public final PacketListenerCommon registrationHandle() {
    return handle;
  }

  public void tryRemovePluginReference() {
    plugin = null;
  }
}
