package de.jpx3.intave.packet.converter;

import de.jpx3.intave.codec.CodecTranslator;
import de.jpx3.intave.codec.StreamCodec;
import de.jpx3.intave.share.PositionMoveRotation;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Translates between Intave's {@link PositionMoveRotation} and the native NMS
 * {@code PositionMoveRotation} record by round-tripping through a {@link ByteBuf} with the matching
 * stream codecs. The {@code getGeneric}/{@code getSpecific}/{@code getSpecificType} surface mirrors the
 * former ProtocolLib {@code EquivalentConverter} contract so the (separately migrated) packet readers
 * and the teleport applier can keep calling it unchanged.
 */
public final class PosMoveRotConverter {
  public static final PosMoveRotConverter INSTANCE = new PosMoveRotConverter();
  private static final ThreadLocal<ByteBuf> caches = ThreadLocal.withInitial(Unpooled::buffer);
  public static final Class<?> nativePositionMoveRotClass = positionMoveRotationClass();
  private static final StreamCodec<ByteBuf, ByteBuf, PositionMoveRotation> intaveCodec = PositionMoveRotation.STREAM_CODEC;
  private static final StreamCodec<ByteBuf, ByteBuf, Object> nativeCodec = (StreamCodec<ByteBuf, ByteBuf, Object>)
    CodecTranslator.translatedCodecOf(nativePositionMoveRotClass);

  private PosMoveRotConverter() {}

  public Object getGeneric(PositionMoveRotation specific) {
    ByteBuf medium = caches.get();
    intaveCodec.encode(medium, specific);
    Object decode = nativeCodec.decode(medium);
    medium.clear();
    return decode;
  }

  public PositionMoveRotation getSpecific(Object generic) {
    ByteBuf medium = caches.get();
    nativeCodec.encode(medium, generic);
    PositionMoveRotation decode = intaveCodec.decode(medium);
    medium.clear();
    return decode;
  }

  private static Class<?> positionMoveRotationClass() {
    try {
      return Class.forName("net.minecraft.world.entity.PositionMoveRotation");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Cannot find PositionMoveRotation class", e);
    }
  }

  public Class<PositionMoveRotation> getSpecificType() {
    return PositionMoveRotation.class;
  }
}
