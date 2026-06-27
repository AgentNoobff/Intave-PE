package de.jpx3.intave.module.linker.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.ProtocolPacketEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import de.jpx3.intave.IntaveLogger;
import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.klass.create.IRXClassFactory;
import de.jpx3.intave.library.asm.Type;
import de.jpx3.intave.module.Module;
import de.jpx3.intave.module.linker.OneForAll;
import de.jpx3.intave.module.linker.OneForOne;
import de.jpx3.intave.module.linker.SubscriptionInstanceProvider;
import de.jpx3.intave.module.linker.packet.tinyprotocol.InjectionService;
import de.jpx3.intave.packet.reader.PacketReader;
import de.jpx3.intave.packet.reader.PacketReaders;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserRepository;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

import static de.jpx3.intave.IntaveControl.IGNORE_CHUNK_PACKETS;

public final class PacketSubscriptionLinker extends Module {
  private static boolean IGNORE_CHAT_PACKETS = false;
  private static boolean IGNORE_SCOREBOARD_TEAM_PACKETS = false;
  private final IntavePlugin plugin;
  private final Map<PacketTypeCommon, SCOWAList<FilteringPacketAdapter>> customEngineListenerMappings = new ConcurrentHashMap<>();
  private final Map<PacketTypeCommon, SCOWAList<FilteringPacketAdapter>> internalPacketListenerMappings = new ConcurrentHashMap<>();
  private final List<WeakReferencePacketAdapter> internalPacketListener = new ArrayList<>();
  private final List<WeakReferencePacketAdapter> externalPacketListener = new ArrayList<>();
  private InjectionService customInjector;

  public PacketSubscriptionLinker(IntavePlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void enable() {
    this.customInjector = new InjectionService(plugin);
    IGNORE_CHAT_PACKETS = IGNORE_SCOREBOARD_TEAM_PACKETS = plugin.getConfig().getBoolean("compatibility.ignore-scoreboard-packets", false);
  }

  @Override
  public void disable() {
    for (WeakReferencePacketAdapter packetListener : internalPacketListener) {
      unlinkAdapter(packetListener);
      packetListener.tryRemovePluginReference();
    }
    internalPacketListener.clear();
    for (WeakReferencePacketAdapter packetListener : externalPacketListener) {
      unlinkAdapter(packetListener);
      packetListener.tryRemovePluginReference();
    }
    externalPacketListener.clear();
    internalPacketListenerMappings.values().forEach(SCOWAList::clear);
    internalPacketListenerMappings.clear();
    customEngineListenerMappings.values().forEach(SCOWAList::clear);
    customEngineListenerMappings.clear();
    customInjector.reset();
    customInjector.uninjectAll();
  }

  public void linkSubscriptionsIn(PacketEventSubscriber subscriber) {
    SubscriptionInstanceProvider<User, ?, PacketEventSubscriber> instanceProvider = instanceProviderFor(subscriber);
    for (Method method : instanceProvider.type().getMethods()) {
      if (methodRequestsSubscription(method)) {
        linkSubscription(instanceProvider, method);
      }
    }
  }

  public void removeSubscriptionsOf(PacketEventSubscriber subscriber) {
    Class<? extends PacketEventSubscriber> subscriberClass = subscriber.getClass();
    for (SCOWAList<FilteringPacketAdapter> value : internalPacketListenerMappings.values()) {
      value.removeIf(localPacketAdapter -> localPacketAdapter.subscriber() != null && localPacketAdapter.subscriber().getClass().equals(subscriberClass));
    }
  }

  public void refreshLinkages() {
    for (WeakReferencePacketAdapter adapter : internalPacketListener) {
      unlinkAdapter(adapter);
    }
    internalPacketListener.clear();
    for (PacketTypeCommon packetType : internalPacketListenerMappings.keySet()) {
      bakeSubscriptions(packetType, internalPacketListenerMappings.get(packetType));
    }
    for (WeakReferencePacketAdapter weakReferencePacketAdapter : externalPacketListener) {
      linkAdapter(weakReferencePacketAdapter);
    }
    customInjector.reset();
    customEngineListenerMappings.forEach(customInjector::setupSubscriptions);
  }

  private void bakeSubscriptions(PacketTypeCommon type, SCOWAList<FilteringPacketAdapter> filteringPacketAdapters) {
    ForwardingPacketAdapter adapter = new ForwardingPacketAdapter(plugin, type, filteringPacketAdapters);
    internalPacketListener.add(adapter);
    linkAdapter(adapter);
  }

  private void linkAdapter(WeakReferencePacketAdapter adapter) {
    adapter.setRegistrationHandle(PacketEvents.getAPI().getEventManager().registerListener(adapter));
  }

  private void unlinkAdapter(WeakReferencePacketAdapter adapter) {
    if (adapter.registrationHandle() != null) {
      PacketEvents.getAPI().getEventManager().unregisterListener(adapter.registrationHandle());
    } else {
      PacketEvents.getAPI().getEventManager().unregisterListener(adapter);
    }
  }

  private boolean methodRequestsSubscription(Method method) {
    return annotatedAsSubscription(method) && validParameters(method) && validModifiers(method);
  }

  private boolean annotatedAsSubscription(Method method) {
    return method.getAnnotation(PacketSubscription.class) != null;
  }

  private final Set<Class<?>> validParameterTypes = new HashSet<>();
  {
    validParameterTypes.add(ProtocolPacketEvent.class);
    validParameterTypes.add(com.github.retrooper.packetevents.event.CancellableEvent.class);
    validParameterTypes.add(User.class);
    validParameterTypes.add(Player.class);
    validParameterTypes.add(PacketReader.class);
    validParameterTypes.add(PacketTypeCommon.class);
  }

  private boolean validParameters(Method method) {
    return (method.getParameterCount() == 1 && method.getParameterTypes()[0] == ProtocolPacketEvent.class) ||
      Arrays.stream(method.getParameterTypes()).allMatch(type -> {
        return validParameterTypes.stream().anyMatch(aClass -> aClass.isAssignableFrom(type));
      });
  }

  private boolean validModifiers(Method method) {
    int modifiers = method.getModifiers();
    return !Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers);
  }

  private void linkSubscription(SubscriptionInstanceProvider<User, ?, PacketEventSubscriber> instanceProvider, Method method) {
    PacketSubscription metadata = method.getAnnotation(PacketSubscription.class);
    PacketSubscriptionMethodExecutor executor = assembleSubscriptionMethodCaller(instanceProvider.type(), method, metadata.identifier());
    String methodName = method.getName();
    ListenerPriority priority = metadata.priority();
    PacketTypeCommon[] packetTypes = translatePacketTypes(metadata.packetsIn(), metadata.packetsOut(), metadata.debug());
    boolean ignoreCancelled = metadata.ignoreCancelled();
    // ASYNC_INTERNAL historically used the TinyProtocol injector; PacketEvents listeners already run
    // asynchronously on the netty threads, so those subscriptions are routed through the normal
    // internal path (see EventTinyProtocol for why the custom dispatch bridge no longer exists).
    if (metadata.engine() == Engine.ASYNC_INTERNAL || metadata.prioritySlot() == PrioritySlot.INTERNAL) {
      performInternalLinkage(instanceProvider, priority, packetTypes, ignoreCancelled, methodName, executor);
    } else {
      performExternalLinkage(instanceProvider, priority, packetTypes, ignoreCancelled, methodName, executor);
    }
  }

  private SubscriptionInstanceProvider<User, ?, PacketEventSubscriber> instanceProviderFor(PacketEventSubscriber subscriber) {
    if (subscriber instanceof PlayerPacketEventSubscriber) {
      PlayerPacketEventSubscriber playerListener = (PlayerPacketEventSubscriber) subscriber;
      return new OneForOne<>(playerListener::packetSubscriberFor);
    } else {
      return new OneForAll<>(subscriber);
    }
  }

  private PacketTypeCommon[] translatePacketTypes(
    PacketId.Client[] clientPackets,
    PacketId.Server[] serverPackets,
    boolean debug
  ) {
    return distinct(
      excludeProblematic(translate(clientPackets, serverPackets, debug), debug),
      PacketTypeCommon[]::new
    );
  }

  private PacketTypeCommon[] translate(PacketId.Client[] clientPackets, PacketId.Server[] serverPackets, boolean debug) {
    List<PacketTypeCommon> list = new ArrayList<>();
    if (clientPackets.length == 1 && "*".equals(clientPackets[0].lookupName())) {
      list.addAll(Arrays.asList(PacketTypeTranslator.allClientTypes()));
    } else {
      for (PacketId.Client clientPacket : clientPackets) {
        PacketTypeCommon[] translated = PacketTypeTranslator.translate(clientPacket);
        if (debug) {
          IntaveLogger.logger().info("Translated " + clientPacket.lookupName() + " to " + Arrays.toString(translated));
        }
        list.addAll(Arrays.asList(translated));
      }
    }
    if (serverPackets.length == 1 && "*".equals(serverPackets[0].lookupName())) {
      list.addAll(Arrays.asList(PacketTypeTranslator.allServerTypes()));
    } else {
      for (PacketId.Server serverPacket : serverPackets) {
        PacketTypeCommon[] translated = PacketTypeTranslator.translate(serverPacket);
        if (debug) {
          IntaveLogger.logger().info("Translated " + serverPacket.lookupName() + " to " + Arrays.toString(translated));
        }
        list.addAll(Arrays.asList(translated));
      }
    }
    return list.toArray(new PacketTypeCommon[0]);
  }

  private <T> T[] distinct(T[] input, IntFunction<T[]> generator) {
    return Arrays.stream(input).filter(Objects::nonNull).distinct().toArray(generator);
  }

  private final Set<String> exclusionNoted = new HashSet<>();

  private PacketTypeCommon[] excludeProblematic(PacketTypeCommon[] input, boolean debug) {
    for (int i = 0; i < input.length; i++) {
      PacketTypeCommon packetType = input[i];
      if (excluded(packetType)) {
        String typeName = packetType.getName();
        if (!exclusionNoted.contains(typeName)) {
          IntaveLogger.logger().info("Ignoring " + typeName + " packets");
        }
        exclusionNoted.add(typeName);
        input[i] = null;
      }
    }
    return input;
  }

  private boolean excluded(PacketTypeCommon packetType) {
    boolean tabChatPacket = packetType == PacketType.Play.Client.TAB_COMPLETE ||
      packetType == PacketType.Play.Server.TAB_COMPLETE ||
      packetType == PacketType.Play.Client.CHAT_MESSAGE;
    if (IGNORE_CHAT_PACKETS && tabChatPacket) {
      return true;
    }
    if (IGNORE_CHUNK_PACKETS && (packetType == PacketType.Play.Server.CHUNK_DATA ||
      packetType == PacketType.Play.Server.MAP_CHUNK_BULK)) {
      return true;
    }
    if (IGNORE_SCOREBOARD_TEAM_PACKETS && (packetType == PacketType.Play.Server.TEAMS)) {
      return true;
    }
    return false;
  }

  private static final ThreadLocal<Map<Integer, Object[]>> argumentCache = ThreadLocal.withInitial(HashMap::new);
  private static final ThreadLocal<Map<Integer, Boolean>> argumentLocks = ThreadLocal.withInitial(HashMap::new);

  private PacketSubscriptionMethodExecutor assembleSubscriptionMethodCaller(
    Class<? extends PacketEventSubscriber> targetClass,
    Method calledMethod,
    String identifier
  ) {
    if (calledMethod.getParameterCount() == 1 && calledMethod.getParameterTypes()[0] == ProtocolPacketEvent.class) {
      String packetSubscriberSuperClassPath = canonicalRepresentation(className(PacketEventSubscriber.class));
      String packetSubscriberClassPath = canonicalRepresentation(className(targetClass));
      String packetEventClassPath = canonicalRepresentation(className(ProtocolPacketEvent.class));
      Class<PacketSubscriptionMethodExecutor> executorClass = IRXClassFactory.assembleCallerClass(
        PacketSubscriptionLinker.class.getClassLoader(),
        PacketSubscriptionMethodExecutor.class,
        "<irx>",
        "invoke",
        "(L" + packetSubscriberSuperClassPath + ";L" + packetEventClassPath + ";)V",
        "(L" + packetSubscriberClassPath + ";L" + packetEventClassPath + ";)V",
        packetSubscriberClassPath,
        calledMethod.getName(),
        Type.getMethodDescriptor(calledMethod),
        false, false,
        IntUnaryOperator.identity()
      );
      return instanceOf(executorClass);
    } else {
      Class<?>[] parameterTypes = calledMethod.getParameterTypes();
      int length = parameterTypes.length;

      int playerParameterIndex = findParameterPosition(parameterTypes, Player.class);
      int userParameterPosition = findParameterPosition(parameterTypes, User.class);
      int cancelableParameterPosition = findParameterPosition(parameterTypes, com.github.retrooper.packetevents.event.CancellableEvent.class);
      int packetReaderParameterPosition = findParameterPosition(parameterTypes, PacketReader.class);
      int packetEventParameterPosition = findParameterPosition(parameterTypes, ProtocolPacketEvent.class);
      int packetTypeParameterPosition = findParameterPosition(parameterTypes, PacketTypeCommon.class);

      AtomicBoolean block = new AtomicBoolean(false);

      return (subscriber, event) -> {
        if (block.get()) {
          return;
        }
        Player player = event.getPlayer();

        Map<Integer, Boolean> locks = argumentLocks.get();
        Boolean isLocked = locks.get(length);
        if (isLocked == null) {
          locks.put(length, true);
          isLocked = false;
        }

        Object[] arguments = isLocked ? new Object[length] : argumentCache.get().computeIfAbsent(length, x -> new Object[length]);

        if (playerParameterIndex != -1) {
          arguments[playerParameterIndex] = player;
        }
        if (userParameterPosition != -1) {
          arguments[userParameterPosition] = UserRepository.userOf(player);
        }
        if (cancelableParameterPosition != -1) {
          arguments[cancelableParameterPosition] = event;
        }
        PacketReader packetReader = null;
        if (packetReaderParameterPosition != -1) {
          try {
            packetReader = PacketReaders.readerOf(event);
            arguments[packetReaderParameterPosition] = packetReader;
          } catch (Exception e) {
            block.set(true);
            IntaveLogger.logger().info(subscriber.getClass().getCanonicalName() + " skipped packet type due to missing reader for " + event.getPacketType().getName());
            return;
          }
        }
        if (packetEventParameterPosition != -1) {
          arguments[packetEventParameterPosition] = event;
        }
        if (packetTypeParameterPosition != -1) {
          arguments[packetTypeParameterPosition] = event.getPacketType();
        }

        try {
          calledMethod.invoke(subscriber, arguments);
        } catch (Exception e) {
          throw new RuntimeException("Failed to invoke packet subscription method " + calledMethod + " in " + subscriber.getClass().getCanonicalName(), e);
        }

        if (packetReader != null) {
          packetReader.releaseSafe();
        }

        if (!isLocked) {
          locks.put(length, false);
          Arrays.fill(arguments, null);
        }
      };
    }
  }

  private static int findParameterPosition(Class<?>[] parameterTypes, Class<?> parameterType) {
    for (int i = 0; i < parameterTypes.length; i++) {
      if (parameterTypes[i] == parameterType) {
        return i;
      }
      if (parameterType.isAssignableFrom(parameterTypes[i])) {
        return i;
      }
    }
    return -1;
  }

  private <T> T instanceOf(Class<T> clazz) {
    try {
      return clazz.newInstance();
    } catch (InstantiationException | IllegalAccessException exception) {
      throw new Error(exception);
    }
  }

  private String className(Class<?> clazz) {
    return clazz.getCanonicalName();
  }

  private String canonicalRepresentation(String input) {
    return input.replaceAll("\\.", "/");
  }

  private static <T> T[] merge(T[] array1, T[] array2) {
    if (array1 == null) {
      return clone(array2);
    } else if (array2 == null) {
      return clone(array1);
    } else {
      //noinspection unchecked
      T[] joinedArray = (T[]) Array.newInstance(array1.getClass().getComponentType(), array1.length + array2.length);
      System.arraycopy(array1, 0, joinedArray, 0, array1.length);
      try {
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
      } catch (ArrayStoreException var6) {
        Class<?> type1 = array1.getClass().getComponentType();
        Class<?> type2 = array2.getClass().getComponentType();
        if (!type1.isAssignableFrom(type2)) {
          throw new IllegalArgumentException("Cannot store " + type2.getName() + " in an array of " + type1.getName());
        } else {
          throw var6;
        }
      }
    }
  }

  private static <T> T[] clone(T[] array) {
    return array == null ? null : array.clone();
  }

  private void performCustomLinkage(
    SubscriptionInstanceProvider<User, ?, PacketEventSubscriber> subscriber,
    ListenerPriority priority, PacketTypeCommon[] translatePacketTypes,
    boolean ignoreCancelled,
    String methodName, PacketSubscriptionMethodExecutor executor
  ) {
    if (translatePacketTypes.length == 0) {
      return;
    }
    FilteringPacketAdapter adapter = new FilteringPacketAdapter(plugin, subscriber, priority, translatePacketTypes, methodName, executor, ignoreCancelled);
    for (PacketTypeCommon translatePacketType : translatePacketTypes) {
      SCOWAList<FilteringPacketAdapter> adapters =
        customEngineListenerMappings.computeIfAbsent(translatePacketType, x -> new SCOWAList<>());
      adapters.add(adapter);
    }
  }

  private void performInternalLinkage(
    SubscriptionInstanceProvider<User, ?, PacketEventSubscriber> subscriber,
    ListenerPriority priority, PacketTypeCommon[] translatePacketTypes,
    boolean ignoreCancelled,
    String methodName, PacketSubscriptionMethodExecutor executor
  ) {
    for (PacketTypeCommon translatePacketType : translatePacketTypes) {
      FilteringPacketAdapter adapter = new FilteringPacketAdapter(plugin, subscriber, priority, new PacketTypeCommon[]{translatePacketType}, methodName, executor, ignoreCancelled);
      internalPacketListenerMappings.computeIfAbsent(translatePacketType, x -> new SCOWAList<>()).add(adapter);
    }
  }

  private void performExternalLinkage(
    SubscriptionInstanceProvider<User, ?, PacketEventSubscriber> subscriber,
    ListenerPriority priority, PacketTypeCommon[] translatePacketTypes,
    boolean ignoreCancelled,
    String methodName, PacketSubscriptionMethodExecutor executor
  ) {
    if (translatePacketTypes.length == 0) {
      return;
    }
    FilteringPacketAdapter adapter = new FilteringPacketAdapter(plugin, subscriber, priority, translatePacketTypes, methodName, executor, ignoreCancelled);
    linkAdapter(adapter);
    externalPacketListener.add(adapter);
  }
}
