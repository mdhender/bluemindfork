package net.bluemind.locator;

import org.vertx.java.platform.Verticle;

import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.IVerticlePriority;

public class LocatorVerticleFactory implements IVerticleFactory, IVerticlePriority {

	public LocatorVerticleFactory() {
	}

	@Override
	public boolean isWorker() {
		return false;
	}

	@Override
	public Verticle newInstance() {
		return new LocatorVerticle();
	}

	@Override
	public int getPriority() {
		return Integer.MAX_VALUE - 1;
	}

}
