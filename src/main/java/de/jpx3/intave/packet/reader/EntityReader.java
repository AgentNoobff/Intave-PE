package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import de.jpx3.intave.entity.EntityLookup;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

public class EntityReader extends AbstractPacketReader implements EntityIterable {
  private PacketWrapper<?> wrapper;

  @Override
  public void release() {
    wrapper = null;
    super.release();
  }

  /**
   * Generic wrapper over the in-flight packet. Almost every entity-related packet (client and
   * server) encodes the entity id as its first VarInt, which is what the legacy ProtocolLib reader
   * read as {@code getIntegers().read(0)}.
   */
  private PacketWrapper<?> wrapper() {
    if (wrapper == null) {
      ProtocolPacketEvent event = event();
      wrapper = event instanceof PacketSendEvent
        ? new PacketWrapper<>((PacketSendEvent) event)
        : new PacketWrapper<>((PacketReceiveEvent) event);
    }
    return wrapper;
  }

  public int entityId() {
    PacketWrapper<?> w = wrapper();
    io.netty.buffer.ByteBuf buffer = (io.netty.buffer.ByteBuf) w.getBuffer();
    if (buffer == null) {
      return -1;
    }
    int readerIndex = buffer.readerIndex();
    buffer.readerIndex(0);
    try {
      return w.readVarInt();
    } finally {
      buffer.readerIndex(readerIndex);
    }
  }

  public @Nullable Entity entityBy(ProtocolPacketEvent event) {
    return entityBy(((org.bukkit.entity.Player) event.getPlayer()).getWorld());
  }

  public @Nullable Entity entityBy(World world) {
    return EntityLookup.findEntity(world, entityId());
  }

  private boolean pendingIdAccess = true;

  @NotNull
  @Override
  public SubstitutionIterator<Integer> iterator() {
    pendingIdAccess = true;
    return STATIC_ITERATOR;
  }

  @Override
  public void forEach(Consumer<? super Integer> action) {
    action.accept(entityId());
  }

  @Override
  public Spliterator<Integer> spliterator() {
    return Spliterators.spliterator(iterator(), 1, 0);
  }

  private final SubstitutionIterator<Integer> STATIC_ITERATOR = new SubstitutionIterator<Integer>() {
    @Override
    public boolean hasNext() {
      return pendingIdAccess;
    }

    @Override
    public Integer next() {
      pendingIdAccess = false;
      return entityId();
    }

    @Override
    public void set(Integer integer) {
      // TODO(pe-migration): generic in-place rewrite of the leading VarInt entity id. PacketEvents
      // has no index-based field write like ProtocolLib's StructureModifier; entity-id remapping
      // (EntityIdFilter) should be reworked onto the concrete typed wrapper for the packet type.
      PacketWrapper<?> w = wrapper();
      io.netty.buffer.ByteBuf buffer = (io.netty.buffer.ByteBuf) w.getBuffer();
      if (buffer == null) {
        return;
      }
      int readerIndex = buffer.readerIndex();
      int writerIndex = buffer.writerIndex();
      buffer.readerIndex(0);
      buffer.writerIndex(0);
      try {
        w.writeVarInt(integer);
      } finally {
        buffer.readerIndex(readerIndex);
        buffer.writerIndex(writerIndex);
      }
      event().markForReEncode(true);
    }
  };
}
