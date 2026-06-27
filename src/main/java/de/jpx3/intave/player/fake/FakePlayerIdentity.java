package de.jpx3.intave.player.fake;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.player.UserProfile;

import java.util.ArrayList;
import java.util.List;

public abstract class FakePlayerIdentity {
  private final int identifier;
  private final UserProfile profile;
  private final List<EntityData<?>> dataWatcher = new ArrayList<>();

  protected FakePlayerIdentity(int identifier, UserProfile profile) {
    this.identifier = identifier;
    this.profile = profile;
  }

  public int identifier() {
    return identifier;
  }

  public UserProfile profile() {
    return profile;
  }

  public List<EntityData<?>> dataWatcher() {
    return dataWatcher;
  }
}
