package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public final class PayloadInReader extends AbstractPacketReader {

  private WrapperPlayClientPluginMessage message() {
    return new WrapperPlayClientPluginMessage((PacketReceiveEvent) event());
  }

  public String tag() {
    String channel = message().getChannelName();
    if (channel == null) {
      return "error";
    }
    if (channel.startsWith("minecraft:")) {
      channel = channel.substring(10);
    }
    return channel;
  }

  public ByteBuf readBytes() {
    return Unpooled.wrappedBuffer(message().getData());
  }

  public String readStringNormal() {
    return new String(message().getData(), StandardCharsets.UTF_8);
  }

  public String readStringWithExtraByte() {
    byte[] data = message().getData();
    if (data.length <= 1) {
      return "";
    }
    // Legacy payloads prefix the string with a length byte; skip it.
    return new String(data, 1, data.length - 1, StandardCharsets.UTF_8);
  }
}
