package de.jpx3.intave.share;

import de.jpx3.intave.klass.locate.Locate;
import de.jpx3.intave.klass.locate.MethodSearchBySignature;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;

public final class FriendlyByteBuf {
  public static ByteBuf from256Unpooled() {
    return wrapping(Unpooled.buffer(256, 2048));
  }

  public static ByteBuf wrapping(ByteBuf byteBuf) {
    try {
      return (ByteBuf) packetDataSerializerConstructor.newInstance(byteBuf);
    } catch (Throwable e) {
      throw new RuntimeException("Cannot construct PacketDataSerializer wrapping the given ByteBuf", e);
    }
  }

  public static String readUtf(ByteBuf friendly, int maxLength) {
    try {
      if (readUtfMethod == null) {
        return "something went wrong";
      }
      return (String) readUtfMethod.invoke(friendly, maxLength);
    } catch (Throwable e) {
      e.printStackTrace();
      return "something went wrong";
    }
  }

  public static void setup() {
  }

  private static final MethodHandle readUtfMethod;
  private static final Constructor<?> packetDataSerializerConstructor;

  static {
    Class<?> packetDataSerializerClass = SpigotReflectionUtil.NMS_PACKET_DATA_SERIALIZER_CLASS;
    try {
      Constructor<?> constructor = packetDataSerializerClass.getConstructor(ByteBuf.class);
      constructor.setAccessible(true);
      packetDataSerializerConstructor = constructor;
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Cannot find PacketDataSerializer(ByteBuf) constructor", e);
    }

    MethodHandle method;
    Class<?> rfbbclassoptional = Locate.classByKey("PacketDataSerializer");
    try {
      method = MethodHandles.lookup().unreflect(rfbbclassoptional.getDeclaredMethod("readUtf", int.class));
    } catch (NoSuchMethodException e) {
      method = MethodSearchBySignature.ofClass(packetDataSerializerClass)
        .withReturnType(String.class)
        .withParameters(new Class[]{int.class})
        .search().findFirst().get();
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    readUtfMethod = method;
  }
}
