package de.jpx3.intave.packet.reader;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;

public final class GameStateChangeReader extends AbstractPacketReader {

  private WrapperPlayServerChangeGameState wrapper() {
    return new WrapperPlayServerChangeGameState((PacketSendEvent) event());
  }

  public GameState type() {
    int index = typeIndex();
    if (index < 0 || index >= GameState.values().length) {
      return GameState.INVALID_BED;
    }
    return GameState.values()[index];
  }

  private int typeIndex() {
    // PacketEvents' Reason enum is ordered identically to the vanilla game-state reason ids, which
    // is the index space the GameState enum below mirrors.
    WrapperPlayServerChangeGameState.Reason reason = wrapper().getReason();
    return reason == null ? -1 : reason.ordinal();
  }

  public float value() {
    return wrapper().getValue();
  }

  public int valueAsInt() {
    return (int)(value() + 0.5F);
  }

  public enum GameState {
    INVALID_BED(0),
    END_RAIN(1),
    BEGIN_RAIN(2),
    CHANGE_GAME_MODE(3),
    ENTER_CREDITS(4),
    DEMO_MESSAGE(5),
    ARROW_HITTING_PLAYER(6),
    RAIN_LEVEL_CHANGE(7),
    THUNDER_LEVEL_CHANGE(8),
    PLAY_MOB_APPEARANCE(9),
    PLAY_MOB2_APPEARANCE(10),
    ENABLE_RESPAWN_SCREEN(11),
    ;

    private final int id;

    GameState(int id) {
      this.id = id;
    }

    public int id() {
      return id;
    }
  }
}
