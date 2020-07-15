package net.bluemind.calendar.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.bluemind.core.container.model.ContainerSyncResult;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.task.service.NullTaskMonitor;

public class CalendarSyncPenaltyTest {

	@Test
	public void testNoPenalty() {
		CalendarSyncException exception = new NoSyncDoneException();
		CalendarContainerSyncTestImpl sync = new CalendarContainerSyncTestImpl(null, null, exception);

		Map<String, String> syncTokens = new HashMap<>();
		long delay = 1000 * 60 * 60 * 24;
		syncTokens.put("current-sync-delay", "" + delay);

		ContainerSyncResult ret = sync.sync(syncTokens, new NullTaskMonitor());

		assertTrue(inRange(delay, ret.status.nextSync));
		assertEquals(exception.getErrorInfo(), ret.status.syncStatusInfo);
		assertEquals(ContainerSyncStatus.Status.SUCCESS, ret.status.syncStatus);
	}

	@Test
	public void testSimplePenalty() {
		CalendarSyncException exception = new ExternalServerException();
		CalendarContainerSyncTestImpl sync = new CalendarContainerSyncTestImpl(null, null, exception);

		Map<String, String> syncTokens = new HashMap<>();
		long delay = 1000 * 60 * 60 * 24;
		syncTokens.put("current-sync-delay", "" + delay);

		ContainerSyncResult ret = sync.sync(syncTokens, new NullTaskMonitor());

		assertTrue(inRange(new Double(delay * 1.5).longValue(), ret.status.nextSync));
		assertEquals(exception.getErrorInfo(), ret.status.syncStatusInfo);
		assertEquals(ContainerSyncStatus.Status.ERROR, ret.status.syncStatus);
	}

	@Test
	public void testCalculatedPenalty() {
		CalendarSyncException exception = new TooManySyncElementsException(150);
		CalendarContainerSyncTestImpl sync = new CalendarContainerSyncTestImpl(null, null, exception);

		Map<String, String> syncTokens = new HashMap<>();
		long delay = 1000 * 60 * 60 * 24;
		syncTokens.put("current-sync-delay", "" + delay);

		ContainerSyncResult ret = sync.sync(syncTokens, new NullTaskMonitor());
		double penalty = 2 - 50d / 150;
		long delay2 = new Double(delay * penalty).longValue();
		assertTrue(inRange(delay2, ret.status.nextSync));
		assertEquals(exception.getErrorInfo(), ret.status.syncStatusInfo);
		assertEquals(ContainerSyncStatus.Status.ERROR, ret.status.syncStatus);
	}

	@Test
	public void testAccumulatedPenalty() {
		CalendarSyncException exception = new ExternalServerException();
		CalendarContainerSyncTestImpl sync = new CalendarContainerSyncTestImpl(null, null, exception);

		Map<String, String> syncTokens = new HashMap<>();
		long delay = 1000 * 60 * 60 * 24;
		syncTokens.put("current-sync-delay", "" + delay);

		ContainerSyncResult ret = sync.sync(syncTokens, new NullTaskMonitor());

		assertTrue(inRange(new Double(delay * 1.5).longValue(), ret.status.nextSync));
		assertEquals(exception.getErrorInfo(), ret.status.syncStatusInfo);
		assertEquals(ContainerSyncStatus.Status.ERROR, ret.status.syncStatus);

		String punishedDelay = ret.status.syncTokens.get("current-sync-delay");
		syncTokens.put("current-sync-delay", punishedDelay);
		ret = sync.sync(syncTokens, new NullTaskMonitor());

		assertTrue(inRange(new Double(Long.parseLong(punishedDelay) * 1.5).longValue(), ret.status.nextSync));
		assertEquals(exception.getErrorInfo(), ret.status.syncStatusInfo);
		assertEquals(ContainerSyncStatus.Status.ERROR, ret.status.syncStatus);

	}

	private boolean inRange(long delay, Long nextSync) {
		long now = System.currentTimeMillis();
		long start = now + delay - 10000;
		long end = now + delay + 10000;

		return nextSync > start && nextSync < end;
	}

}
