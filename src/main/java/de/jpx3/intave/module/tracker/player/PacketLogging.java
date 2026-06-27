package de.jpx3.intave.module.tracker.player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.cleanup.GarbageCollector;
import de.jpx3.intave.cleanup.ShutdownTasks;
import de.jpx3.intave.module.Module;
import de.jpx3.intave.user.User;
import io.netty.buffer.ByteBuf;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

public class PacketLogging extends Module {

  private final Map<UUID, PacketListenerCommon> adapterMap = GarbageCollector.watch(new HashMap<>());
  private final Map<String, UUID> packetLoggers = GarbageCollector.watch(new HashMap<>());
  private final Map<UUID, PrintStream> packetLogStreams = GarbageCollector.watch(new HashMap<>());

  {
    ShutdownTasks.add(() -> {
      packetLogStreams.forEach((uuid, printStream) -> {
        printStream.flush();
        printStream.close();
      });
    });
  }

  // PacketEvents fires send/receive only once the player is a real, fully-connected PLAY-phase
  // player with a Bukkit handle, which is the equivalent of ProtocolLib's !isPlayerTemporary().
  private boolean isTemporary(ProtocolPacketEvent event) {
    return event.getConnectionState() != ConnectionState.PLAY || event.getPlayer() == null;
  }

  public void togglePacketLogging(CommandSender sender, Player target) {
    File logsFolder = new File(plugin.dataFolder(), "packetlogs");
    File packetLogFile = new File(logsFolder, packetLogFileName(target.getName()));

    UUID userId = target.getUniqueId();
    if (packetLoggers.containsKey(sender.getName())) {
      if (!packetLoggers.get(sender.getName()).equals(userId)) {
        sender.sendMessage(IntavePlugin.prefix() + ChatColor.GREEN + "You currently can only packetlog one player at the time, contact us if you need to log multiple players at the same time.");
        sender.sendMessage(IntavePlugin.prefix() + ChatColor.GREEN + "We will stop packetlogging for " + packetLoggers.get(sender.getName()));
        userId = packetLoggers.get(sender.getName());
      } else {
        sender.sendMessage(IntavePlugin.prefix() + ChatColor.GREEN + "Packetlogging stopped");
      }
      PacketListenerCommon remove1 = adapterMap.remove(userId);
      if (remove1 != null) {
        PacketEvents.getAPI().getEventManager().unregisterListener(remove1);
      }
      packetLoggers.remove(sender.getName());
      PrintStream remove = packetLogStreams.remove(userId);
      if (remove != null) {
        remove.flush();
        remove.close();
      }
      return;
    }

    try {
      logsFolder.mkdir();
      packetLogFile.createNewFile();
    } catch (IOException exception) {
      exception.printStackTrace();
      return;
    }

    try {
      OutputStream stream = new FileOutputStream(packetLogFile);
      stream = new BufferedOutputStream(stream);
      PrintStream printStream = new PrintStream(stream);

      UUID finalUserId = userId;
      PacketListenerAbstract adapter = new PacketListenerAbstract(PacketListenerPriority.MONITOR) {
        @Override
        public void onPacketSend(PacketSendEvent event) {
          if (isTemporary(event)) {
            return;
          }
          Player player = event.getPlayer();
          if (player.getUniqueId().equals(finalUserId)) {
            synchronized (printStream) {
              printStream.println((System.currentTimeMillis() % 1000) + " <--out-- " + event.getPacketType().getName() + (event.isCancelled() ? " (cancelled)" : "") + " " + packetContent(event));
            }
          }
        }

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
          if (isTemporary(event)) {
            return;
          }
          Player player = event.getPlayer();
          if (player.getUniqueId().equals(finalUserId)) {
            synchronized (printStream) {
              printStream.println((System.currentTimeMillis() % 1000) + " --in--> " + event.getPacketType().getName() + (event.isCancelled() ? " (cancelled)" : "") + " " + packetContent(event));
            }
          }
        }
      };
      PacketListenerCommon handle = PacketEvents.getAPI().getEventManager().registerListener(adapter);
      adapterMap.put(userId, handle);
      packetLoggers.put(sender.getName(), userId);
      packetLogStreams.put(userId, printStream);
    } catch (FileNotFoundException exception) {
      exception.printStackTrace();
    }
    sender.sendMessage(IntavePlugin.prefix() + ChatColor.GREEN + "Packetlogging started for " + target.getName());
    sender.sendMessage(IntavePlugin.prefix() + "You can find it under " + packetLogFile.getAbsolutePath());
  }

  public void logSystemMessage(User target, Supplier<String> messageSupplier) {
    if (target == null) {
      return;
    }
    boolean requestedMovementDebugToConsole = System.currentTimeMillis() - target.meta().violationLevel().lastMovementDebugRequest < 10_000;
    PrintStream stream;
    try {
      stream = packetLogStreams.get(target.player().getUniqueId());
      if (stream == null) {
        if (requestedMovementDebugToConsole) {
          String message = messageSupplier.get();
          plugin.logTransmittor().addPlayerLog(target.player(), "MOVE_DEBUG> " + message);
        }
        return;
      }
    } catch (Exception exception) {
      return;
    }
    synchronized (stream) {
      stream.println((System.currentTimeMillis() % 1000) + " " + messageSupplier.get());
    }
  }

  // TODO(pe-migration): ProtocolLib exposed every decoded packet field via StructureModifier, which let
  // this dump pretty-print each value. PacketEvents has no generic per-type field reflection; without a
  // typed wrapper per packet we can only surface the raw wire bytes. Dumps the buffer as a byte preview.
  private static String packetContent(ProtocolPacketEvent event) {
    try {
      PacketWrapper<?> wrapper = event instanceof PacketSendEvent
        ? new PacketWrapper<>((PacketSendEvent) event)
        : new PacketWrapper<>((PacketReceiveEvent) event);
      ByteBuf buffer = (ByteBuf) wrapper.getBuffer();
      if (buffer == null) {
        return "{}";
      }
      int readable = buffer.readableBytes();
      int limit = Math.min(readable, 40);
      StringBuilder builder = new StringBuilder("{bytes=[");
      int start = buffer.readerIndex();
      for (int i = 0; i < limit; i++) {
        builder.append(buffer.getByte(start + i));
        if (i != limit - 1) {
          builder.append(", ");
        }
      }
      if (readable > 40) {
        builder.append("...");
      }
      builder.append("]}");
      return builder.toString();
    } catch (Exception exception) {
      return "{error: " + exception.getClass().getSimpleName() + "}";
    }
  }

  private static final DateTimeFormatter FILE_MESSAGE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss");

  private static String packetLogFileName(String playername) {
    return "intave-packetlog-" + playername + "-" + LocalDateTime.now().format(FILE_MESSAGE_DATE_FORMATTER).toLowerCase(Locale.ROOT) + ".txt";
  }
}
