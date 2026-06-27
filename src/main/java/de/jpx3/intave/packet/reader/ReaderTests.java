package de.jpx3.intave.packet.reader;

import de.jpx3.intave.test.IntegrationTests;
import de.jpx3.intave.test.Severity;
import de.jpx3.intave.test.Test;

public final class ReaderTests extends IntegrationTests {
  public ReaderTests() {
    super("PR");
  }

  @Test(
    testCode = "A",
    severity = Severity.ERROR
  )
  public void testAllPlayerInfoReaders() {
    // TODO(pe-migration): the legacy smoke test constructed empty ProtocolLib PacketContainers for
    // every readable type and reflectively invoked each reader. PacketEvents has no way to build a
    // synthetic packet event without a live connection, so reader coverage must now be exercised via
    // the in-game integration runs (gradlew run_*) rather than here.
  }
}
