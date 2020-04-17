package net.bluemind.scheduledjob.scheduler.impl;

public interface CancellableRunnable extends Runnable {

	public void cancel();
}
