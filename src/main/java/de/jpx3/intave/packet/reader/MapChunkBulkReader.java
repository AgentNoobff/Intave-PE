package de.jpx3.intave.packet.reader;

public final class MapChunkBulkReader extends AbstractPacketReader implements ChunkCoordinateReader {
  // TODO(pe-migration): MAP_CHUNK_BULK is a 1.8-only packet with no PacketEvents wrapper, so bulk
  // chunk-cache invalidation on 1.8 is skipped. Consumers must tolerate the empty coordinate arrays.
  @Override
  public int[] xCoordinates() {
    return new int[0];
  }

  @Override
  public int[] zCoordinates() {
    return new int[0];
  }
}
