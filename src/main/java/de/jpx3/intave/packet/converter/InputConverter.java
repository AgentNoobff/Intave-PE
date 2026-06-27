package de.jpx3.intave.packet.converter;

import de.jpx3.intave.codec.CodecTranslator;
import de.jpx3.intave.codec.StreamCodec;
import de.jpx3.intave.share.FriendlyByteBuf;
import de.jpx3.intave.share.Input;
import io.netty.buffer.ByteBuf;

/**
 * Translates between Intave's {@link Input} and the native NMS {@code Input} record by round-tripping
 * through a {@link ByteBuf} with the matching stream codecs. The {@code getGeneric}/{@code getSpecific}/
 * {@code getSpecificType} surface mirrors the former ProtocolLib {@code EquivalentConverter} contract so
 * the (separately migrated) packet readers can keep calling it unchanged.
 */
public final class InputConverter {
	public static final InputConverter INSTANCE = new InputConverter();
	public static final Class<?> inputClass = inputClass();
	private static final ThreadLocal<ByteBuf> caches = ThreadLocal.withInitial(FriendlyByteBuf::from256Unpooled);
	private static final StreamCodec<ByteBuf, ByteBuf, Input> intaveCodec = Input.STREAM_CODEC;
	private static final StreamCodec<ByteBuf, ByteBuf, Object> nativeCodec = (StreamCodec<ByteBuf, ByteBuf, Object>)
		CodecTranslator.translatedCodecOf(inputClass);

	public Object getGeneric(Input input) {
		ByteBuf cache = caches.get();
		cache.clear();
		intaveCodec.encode(cache, input);
		return nativeCodec.decode(cache);
	}

	public Input getSpecific(Object o) {
	  ByteBuf cache = caches.get();
		cache.clear();
		nativeCodec.encode(cache, o);
		return intaveCodec.decode(cache);
	}

	public Class<Input> getSpecificType() {
		return Input.class;
	}

	private static Class<?> inputClass() {
		try {
			return Class.forName("net.minecraft.world.entity.player.Input");
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Cannot find net.minecraft.world.entity.player.Input class", e);
		}
	}
}
