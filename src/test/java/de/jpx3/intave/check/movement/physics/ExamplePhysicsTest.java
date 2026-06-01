package de.jpx3.intave.check.movement.physics;

import de.jpx3.intave.adapter.MinecraftVersion;
import de.jpx3.intave.adapter.MinecraftVersions;
import de.jpx3.intave.block.cache.MockFullBlockStaticPlane;
import de.jpx3.intave.block.fluid.FluidFlow;
import de.jpx3.intave.block.fluid.Fluids;
import de.jpx3.intave.player.collider.Colliders;
import de.jpx3.intave.player.collider.complex.Collider;
import de.jpx3.intave.player.collider.simple.SimpleCollider;
import de.jpx3.intave.share.Motion;
import de.jpx3.intave.test.FakePlayerFactory;
import de.jpx3.intave.test.MockEmptyInventory;
import de.jpx3.intave.user.User;
import de.jpx3.intave.user.UserFactory;
import de.jpx3.intave.user.UserRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.Collections;
import java.util.UUID;

public final class ExamplePhysicsTest {
  private static final UUID EMPTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private User testUser;
  private Player player;

  private final Collider collider = Colliders.anyCollider();
  private final FluidFlow waterflow = Fluids.anyWaterflow();
  private final SimpleCollider simpleCollider = Colliders.anySimpleCollider();
  private final PlayerInventory inventory = new MockEmptyInventory();

//  @BeforeEach
  void setUp() {
    MinecraftVersion.setCurrentVersion(MinecraftVersions.VER1_21_4);
    com.comphenix.protocol.utility.MinecraftVersion.setCurrentVersion(com.comphenix.protocol.utility.MinecraftVersion.v1_21_4);

    player = FakePlayerFactory.createPlayer(
      (s, objects) -> {
        switch (s) {
          case "getInventory":
            return inventory;
          case "getWorld":
            return Bukkit.getWorlds().get(0);
          case "getUniqueId":
            return EMPTY_ID;
          case "getActivePotionEffects":
            return Collections.emptyList();
        }
        return null;
      }
    );

    MockFullBlockStaticPlane plane = new MockFullBlockStaticPlane();
    plane.horizontalFill(1);
    testUser = UserFactory.createTestUserFor(player, s -> {
      switch (s) {
        case "collider":
          return collider;
        case "waterflow":
          return waterflow;
        case "simplifiedCollider":
          return simpleCollider;
        case "blockCache":
          return plane;
        case "protocolVersion":
          return 47;
      }
      return null;
    });
    UserRepository.manuallyRegisterUser(player, testUser);
  }

//  @Test
  public void testy() {
    Simulator simulator = Simulators.PLAYER;
    System.out.println(simulator.stepHeight());
    simulator.simulateTick(
      testUser, Motion.newEmpty(), testUser.meta().movement(), MovementConfiguration.noAction().withJump()
    );
  }
}
