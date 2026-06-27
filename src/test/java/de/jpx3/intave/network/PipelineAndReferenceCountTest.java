package de.jpx3.intave.network;

import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import de.jpx3.intave.IntavePlugin;
import de.jpx3.intave.module.linker.packet.tinyprotocol.TinyProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

public final class PipelineAndReferenceCountTest {

    private final java.util.List<Object> keepAlive = new java.util.ArrayList<>();
    private static sun.misc.Unsafe unsafeInstance;

    static {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafeInstance = (sun.misc.Unsafe) unsafeField.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Object handleByteBufOperator(String name, Object[] args) {
        if (args == null || args.length == 0 || args[0] == null) {
            return null;
        }
        io.netty.buffer.ByteBuf buf = (io.netty.buffer.ByteBuf) args[0];
        if (name.equals("writeVarInt")) {
            int value = (Integer) args[1];
            while ((value & 0xFFFFFF80) != 0L) {
                buf.writeByte((value & 0x7F) | 0x80);
                value >>>= 7;
            }
            buf.writeByte(value & 0x7F);
            return null;
        }
        if (name.equals("readVarInt")) {
            int value = 0;
            int position = 0;
            byte currentByte;
            while (true) {
                currentByte = buf.readByte();
                value |= (currentByte & 0x7F) << position;
                if ((currentByte & 0x80) == 0) {
                    break;
                }
                position += 7;
                if (position >= 32) {
                    throw new RuntimeException("VarInt is too big");
                }
            }
            return value;
        }
        if (name.equals("writeShort")) {
            buf.writeShort((Integer) args[1]);
            return null;
        }
        if (name.equals("readShort")) {
            return buf.readShort();
        }
        if (name.equals("writeInt")) {
            buf.writeInt((Integer) args[1]);
            return null;
        }
        if (name.equals("readInt")) {
            return buf.readInt();
        }
        if (name.equals("writeByte")) {
            buf.writeByte((Integer) args[1]);
            return null;
        }
        if (name.equals("readByte")) {
            return buf.readByte();
        }
        if (name.equals("writeBytes")) {
            if (args[1] instanceof byte[]) {
                buf.writeBytes((byte[]) args[1]);
            } else if (args[1] instanceof io.netty.buffer.ByteBuf) {
                buf.writeBytes((io.netty.buffer.ByteBuf) args[1]);
            }
            return null;
        }
        if (name.equals("clear")) {
            io.netty.util.ReferenceCountUtil.release(buf);
            return null;
        }
        return null;
    }

    private static Object createByteBuddyMock(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        if (clazz == String.class) {
            return "";
        }
        if (clazz.isPrimitive()) {
            if (clazz == boolean.class) return false;
            if (clazz == byte.class) return (byte) 0;
            if (clazz == char.class) return '\0';
            if (clazz == short.class) return (short) 0;
            if (clazz == int.class) return 0;
            if (clazz == long.class) return 0L;
            if (clazz == float.class) return 0.0f;
            if (clazz == double.class) return 0.0d;
            return null;
        }
        if (clazz.isArray()) {
            return java.lang.reflect.Array.newInstance(clazz.getComponentType(), 0);
        }
        if (clazz.isEnum()) {
            Object[] constants = clazz.getEnumConstants();
            if (constants != null && constants.length > 0) {
                return constants[0];
            }
            return null;
        }
        if (java.lang.reflect.Modifier.isFinal(clazz.getModifiers())) {
            try {
                return unsafeInstance.allocateInstance(clazz);
            } catch (Exception e) {
                return null;
            }
        }
        try {
            Class<?> mockClass = new net.bytebuddy.ByteBuddy()
                .subclass(clazz)
                .method(net.bytebuddy.matcher.ElementMatchers.any())
                .intercept(net.bytebuddy.implementation.InvocationHandlerAdapter.of((proxy, method, args) -> {
                    Class<?> returnType = method.getReturnType();
                    String name = method.getName();
                    System.out.println("MOCK CALL: " + method.getDeclaringClass().getName() + "#" + name + " -> " + returnType.getName());
                    if (method.getDeclaringClass().getName().contains("ByteBufOperator")) {
                        return handleByteBufOperator(name, args);
                    }

                    if (name.equals("toString")) {
                        return "MockProxy_" + clazz.getSimpleName();
                    }
                    if (name.equals("equals")) {
                        return args.length > 0 && args[0] == proxy;
                    }
                    if (name.equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (io.netty.buffer.ByteBuf.class.isAssignableFrom(returnType)) {
                        return io.netty.buffer.Unpooled.buffer();
                    }
                    // Recursively mock PacketEvents interfaces/classes and other interfaces
                    if (returnType.isInterface() || returnType.getName().startsWith("com.github.retrooper.packetevents")) {
                        return createByteBuddyMock(returnType);
                    }
                    if (returnType == String.class) {
                        return "";
                    }
                    if (returnType.isPrimitive()) {
                        if (returnType == boolean.class) return false;
                        if (returnType == int.class) return 0;
                        if (returnType == long.class) return 0L;
                    }
                    return null;
                }))
                .make()
                .load(clazz.getClassLoader() != null ? clazz.getClassLoader() : PipelineAndReferenceCountTest.class.getClassLoader())
                .getLoaded();
            return unsafeInstance.allocateInstance(mockClass);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static {
        // 1. Initialize Bukkit Server Mock using DummyCraftServer to force correct package
        try {
            Class<?> dummyServerInterface = Class.forName("org.bukkit.craftbukkit.v1_8_R3.DummyCraftServer");
            org.bukkit.Server mockServer = (org.bukkit.Server) Proxy.newProxyInstance(
                dummyServerInterface.getClassLoader(),
                new Class<?>[]{dummyServerInterface},
                (proxy, method, args) -> {
                    Class<?> returnType = method.getReturnType();
                    String name = method.getName();

                    if (name.equals("getLogger")) {
                        return java.util.logging.Logger.getGlobal();
                    }
                    if (name.equals("getVersion") || name.equals("getBukkitVersion")) {
                        return "git-Spigot-db6de12-18fbb24 (MC: 1.8.8)";
                    }
                    if (name.equals("getName")) {
                        return "CraftServer";
                    }
                    if (name.equals("getClass")) {
                        return dummyServerInterface;
                    }
                    if (returnType.isInterface()) {
                        return createByteBuddyMock(returnType);
                    }
                    if (returnType == String.class) {
                        return "";
                    }
                    if (returnType.isPrimitive()) {
                        if (returnType == boolean.class) return false;
                        if (returnType == int.class) return 0;
                        if (returnType == long.class) return 0L;
                    }
                    return null;
                }
            );
            org.bukkit.Bukkit.setServer(mockServer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Set MinecraftVersion current version directly
        try {
            de.jpx3.intave.adapter.MinecraftVersion.setCurrent(new de.jpx3.intave.adapter.MinecraftVersion(1, 8, 8));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Initialize PacketEvents API Mock using ByteBuddy & Unsafe on the correct ClassLoader
        try {
            Class<?> apiClass = Class.forName("com.github.retrooper.packetevents.PacketEventsAPI", true, PacketWrapper.class.getClassLoader());
            Object mockApi = createByteBuddyMock(apiClass);

            Class<?> packetEventsClass = Class.forName("com.github.retrooper.packetevents.PacketEvents", true, PacketWrapper.class.getClassLoader());

            Field apiField = packetEventsClass.getDeclaredField("API");
            apiField.setAccessible(true);

            Object staticFieldBase = unsafeInstance.staticFieldBase(apiField);
            long staticFieldOffset = unsafeInstance.staticFieldOffset(apiField);
            unsafeInstance.putObject(staticFieldBase, staticFieldOffset, mockApi);

            Method getAPIMethod = packetEventsClass.getDeclaredMethod("getAPI");
            getAPIMethod.setAccessible(true);
            System.out.println("Set API. getAPI() = " + getAPIMethod.invoke(null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class TestTinyProtocol extends TinyProtocol {
        public volatile Object nextInboundResult;
        public volatile Object nextOutboundResult;
        public volatile boolean overrideInbound;
        public volatile boolean overrideOutbound;

        public TestTinyProtocol(IntavePlugin plugin) {
            super(plugin);
        }

        @Override
        protected String handlerName() {
            return "test-handler";
        }

        @Override
        public void injectPlayer(Player player) {
            System.out.println("DEBUG: injectPlayer entry");
            try {
                io.netty.channel.Channel channel = getChannel(player);
                System.out.println("DEBUG: injectPlayer channel: " + channel);
                if (channel == null) {
                    System.out.println("DEBUG: injectPlayer channel is null!");
                    return;
                }
                System.out.println("DEBUG: injectPlayer channel eventLoop: " + channel.eventLoop());
                channel.eventLoop().execute(() -> {
                    System.out.println("DEBUG: EventLoop task starting execution");
                    try {
                        java.lang.reflect.Method method = TinyProtocol.class.getDeclaredMethod("injectChannelInternal", io.netty.channel.Channel.class);
                        method.setAccessible(true);
                        System.out.println("DEBUG: Reflectively invoking injectChannelInternal");
                        Object interceptor = method.invoke(this, channel);
                        System.out.println("DEBUG: injectChannelInternal returned: " + interceptor);
                        if (interceptor != null) {
                            java.lang.reflect.Field playerField = interceptor.getClass().getDeclaredField("player");
                            playerField.setAccessible(true);
                            playerField.set(interceptor, player);
                            System.out.println("DEBUG: Successfully set player on interceptor");
                        }
                    } catch (Throwable t) {
                        System.out.println("DEBUG: EXCEPTION IN EVENT LOOP TASK: " + t);
                        if (t instanceof java.lang.reflect.InvocationTargetException) {
                            System.out.println("DEBUG: Target exception: " + ((java.lang.reflect.InvocationTargetException) t).getTargetException());
                            ((java.lang.reflect.InvocationTargetException) t).getTargetException().printStackTrace();
                        } else {
                            t.printStackTrace();
                        }
                    }
                });
            } catch (Throwable t) {
                System.out.println("DEBUG: EXCEPTION IN injectPlayer entry: " + t);
                t.printStackTrace();
            }
        }

        @Override
        public Object onPacketInAsync(Player sender, Channel channel, Object packet) {
            if (overrideInbound) {
                return nextInboundResult;
            }
            return super.onPacketInAsync(sender, channel, packet);
        }

        @Override
        public Object onPacketOutAsync(Player receiver, Channel channel, Object packet) {
            if (overrideOutbound) {
                return nextOutboundResult;
            }
            return super.onPacketOutAsync(receiver, channel, packet);
        }
    }

    @Test
    void testDirectIndexManipulationLeavesRefCountIntact() {
        // Create a new PacketWrapper using JOIN_GAME packet type
        PacketWrapper<?> wrapper = new PacketWrapper<>(PacketType.Play.Server.JOIN_GAME);
        ByteBuf buffer = Unpooled.buffer();
        wrapper.setBuffer(buffer);

        // Write a VarInt to the buffer first
        wrapper.writeVarInt(12345);

        int initialRef = buffer.refCnt();
        int originalReaderIndex = buffer.readerIndex();

        // Simulate our direct reader index manipulation (as done in EntityReader and EntityTracker)
        buffer.readerIndex(0);
        try {
            int readVal = wrapper.readVarInt();
            assertEquals(12345, readVal);
        } finally {
            buffer.readerIndex(originalReaderIndex);
        }

        // Verify reference count is completely untouched
        assertEquals(initialRef, buffer.refCnt());
        // Verify reader index is restored
        assertEquals(originalReaderIndex, buffer.readerIndex());

        // Cleanup
        buffer.release();
    }

    @Test
    void testPacketWrapperResetByteBufDropsRefCount() {
        // Create a new PacketWrapper using JOIN_GAME packet type
        PacketWrapper<?> wrapper = new PacketWrapper<>(PacketType.Play.Server.JOIN_GAME);
        ByteBuf buffer = Unpooled.buffer();
        wrapper.setBuffer(buffer);

        // Write a VarInt to trigger buffer usage
        wrapper.writeVarInt(12345);

        int initialRef = buffer.refCnt();

        // Calling resetByteBuf() should release the internal buffer, reducing its ref count
        wrapper.resetByteBuf();

        // The original buffer's reference count should be decremented
        assertEquals(initialRef - 1, buffer.refCnt());
    }

    @Test
    void testInboundPacketCancellationReleasesBuffer() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        keepAlive.add(channel);
        // Add dummy anchor to pipeline so injection resolves immediately
        channel.pipeline().addLast("packet_handler", new io.netty.channel.ChannelInboundHandlerAdapter());

        TestTinyProtocol tinyProtocol = new TestTinyProtocol(null);

        // Register channel in lookup map to bypass reflection
        Field field = TinyProtocol.class.getDeclaredField("channelLookup");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Channel> channelLookup = (Map<String, Channel>) field.get(tinyProtocol);
        channelLookup.put("TestPlayer", channel);

        // Create mock player proxy
        Player mockPlayer = (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[]{Player.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getName")) {
                    return "TestPlayer";
                }
                return null;
            }
        );

        System.out.println("DEBUG: channelLookup contents: " + channelLookup);
        System.out.println("DEBUG: player name: " + mockPlayer.getName());
        System.out.println("DEBUG: channelLookup.get: " + channelLookup.get("TestPlayer"));
        try {
            System.out.println("DEBUG: getChannel: " + tinyProtocol.getChannel(mockPlayer));
        } catch (Throwable t) {
            System.out.println("DEBUG: getChannel threw: " + t);
        }
        System.out.println("DEBUG: pipeline names before inject: " + channel.pipeline().names());

        System.out.println("DEBUG: Starting player injection");
        // Inject player (runs asynchronously on event loop)
        tinyProtocol.injectPlayer(mockPlayer);
        System.out.println("DEBUG: Running pending tasks");
        channel.runPendingTasks();
        System.out.println("DEBUG: runPendingTasks completed");
        System.out.println("DEBUG: pipeline names after inject & runPending: " + channel.pipeline().names());

        // Verify injection
        assertNotNull(channel.pipeline().get("test-handler"));

        // Scenario A: Passthrough
        ByteBuf passPacket = Unpooled.buffer();
        passPacket.writeInt(100);
        assertEquals(1, passPacket.refCnt());

        tinyProtocol.overrideInbound = false;
        channel.writeInbound(passPacket);

        // Packet should pass through
        Object readPass = channel.readInbound();
        assertNotNull(readPass);
        assertSame(passPacket, readPass);
        assertEquals(1, passPacket.refCnt());
        passPacket.release(); // clean up

        // Scenario B: Cancellation (returns null)
        ByteBuf cancelPacket = Unpooled.buffer();
        cancelPacket.writeInt(200);
        assertEquals(1, cancelPacket.refCnt());

        tinyProtocol.overrideInbound = true;
        tinyProtocol.nextInboundResult = null;
        channel.writeInbound(cancelPacket);

        // Packet should be swallowed and released
        Object readCancel = channel.readInbound();
        assertNull(readCancel);
        assertEquals(0, cancelPacket.refCnt());
        assertNotNull(keepAlive);
    }

    @Test
    void testOutboundPacketCancellationReleasesBuffer() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();
        keepAlive.add(channel);
        // Add dummy anchor
        channel.pipeline().addLast("packet_handler", new io.netty.channel.ChannelInboundHandlerAdapter());

        TestTinyProtocol tinyProtocol = new TestTinyProtocol(null);

        // Register channel in lookup map
        Field field = TinyProtocol.class.getDeclaredField("channelLookup");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Channel> channelLookup = (Map<String, Channel>) field.get(tinyProtocol);
        channelLookup.put("TestPlayer", channel);

        // Create mock player proxy
        Player mockPlayer = (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[]{Player.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getName")) {
                    return "TestPlayer";
                }
                return null;
            }
        );

        // Inject
        tinyProtocol.injectPlayer(mockPlayer);
        channel.runPendingTasks();

        // Verify injection
        assertNotNull(channel.pipeline().get("test-handler"));

        // Scenario A: Passthrough
        ByteBuf passPacket = Unpooled.buffer();
        passPacket.writeInt(300);
        assertEquals(1, passPacket.refCnt());

        tinyProtocol.overrideOutbound = false;
        channel.writeOutbound(passPacket);

        Object readPass = channel.readOutbound();
        assertNotNull(readPass);
        assertSame(passPacket, readPass);
        assertEquals(1, passPacket.refCnt());
        passPacket.release(); // clean up

        // Scenario B: Cancellation
        ByteBuf cancelPacket = Unpooled.buffer();
        cancelPacket.writeInt(400);
        assertEquals(1, cancelPacket.refCnt());

        tinyProtocol.overrideOutbound = true;
        tinyProtocol.nextOutboundResult = null;
        channel.writeOutbound(cancelPacket);

        Object readCancel = channel.readOutbound();
        assertNull(readCancel);
        assertEquals(0, cancelPacket.refCnt());
        assertNotNull(keepAlive);
    }

    @Test
    void testPipelineInjectionRetryAndFallbackOnNoSuchElementException() throws Exception {
        final int[] addBeforeCount = {0};
        final boolean[] addLastCalled = {false};

        EmbeddedChannel baseChannel = new EmbeddedChannel();
        keepAlive.add(baseChannel);
        ChannelPipeline originalPipeline = baseChannel.pipeline();

        // Create a proxy for ChannelPipeline to intercept addBefore and addLast
        ChannelPipeline proxyPipeline = (ChannelPipeline) Proxy.newProxyInstance(
            ChannelPipeline.class.getClassLoader(),
            new Class<?>[]{ChannelPipeline.class},
            (proxy, method, args) -> {
                if (method.getName().equals("addBefore")) {
                    addBeforeCount[0]++;
                    throw new NoSuchElementException("packet_handler");
                }
                if (method.getName().equals("addLast")) {
                    if (args.length > 0 && args[0].equals("test-handler")) {
                        addLastCalled[0] = true;
                    }
                }
                return method.invoke(originalPipeline, args);
            }
        );

        // Custom EmbeddedChannel overriding pipeline()
        EmbeddedChannel customChannel = new EmbeddedChannel() {
            @Override
            public ChannelPipeline pipeline() {
                return proxyPipeline;
            }
        };
        keepAlive.add(customChannel);

        // Add dummy anchor so resolveAnchor doesn't return null
        customChannel.pipeline().addLast("packet_handler", new io.netty.channel.ChannelInboundHandlerAdapter());

        TestTinyProtocol tinyProtocol = new TestTinyProtocol(null);

        // Register channel in lookup map
        Field field = TinyProtocol.class.getDeclaredField("channelLookup");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Channel> channelLookup = (Map<String, Channel>) field.get(tinyProtocol);
        channelLookup.put("TestPlayer", customChannel);

        // Mock player
        Player mockPlayer = (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[]{Player.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getName")) {
                    return "TestPlayer";
                }
                return null;
            }
        );

        // Trigger injection
        tinyProtocol.injectPlayer(mockPlayer);

        // Run pending tasks for the first attempt (attempt 0)
        customChannel.runPendingTasks();
        assertEquals(1, addBeforeCount[0]);
        assertFalse(addLastCalled[0]);

        // Advance time and run scheduled tasks for the second attempt (attempt 1)
        Thread.sleep(60);
        customChannel.runPendingTasks();
        assertEquals(2, addBeforeCount[0]);
        assertFalse(addLastCalled[0]);

        // Advance time and run scheduled tasks for the third attempt (attempt 2)
        Thread.sleep(60);
        customChannel.runPendingTasks();
        assertEquals(3, addBeforeCount[0]);
        assertFalse(addLastCalled[0]);

        // Advance time and run scheduled tasks for the fourth attempt (attempt 3 - fallback to addLast)
        Thread.sleep(60);
        customChannel.runPendingTasks();
        assertTrue(addLastCalled[0]);
        assertNotNull(keepAlive);
    }

    @Test
    void testPrintPacketWrapperSignatures() {
        System.out.println("=== PACKETWRAPPER CONSTRUCTORS ===");
        for (java.lang.reflect.Constructor<?> c : PacketWrapper.class.getDeclaredConstructors()) {
            System.out.println(c.toString());
        }
        System.out.println("=== PACKETWRAPPER METHODS ===");
        for (java.lang.reflect.Method m : PacketWrapper.class.getDeclaredMethods()) {
            if (m.getName().contains("buffer") || m.getName().contains("Buffer") || m.getName().contains("ByteBuf") || m.getName().contains("set") || m.getName().contains("wrap")) {
                System.out.println(m.toString());
            }
        }
    }

    @org.junit.jupiter.api.AfterAll
    static void tearDownAll() {
        try {
            Field serverField = org.bukkit.Bukkit.class.getDeclaredField("server");
            serverField.setAccessible(true);
            serverField.set(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Field apiField = Class.forName("com.github.retrooper.packetevents.PacketEvents", true, PacketWrapper.class.getClassLoader()).getDeclaredField("API");
            apiField.setAccessible(true);
            Object staticFieldBase = unsafeInstance.staticFieldBase(apiField);
            long staticFieldOffset = unsafeInstance.staticFieldOffset(apiField);
            unsafeInstance.putObject(staticFieldBase, staticFieldOffset, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
