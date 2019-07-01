package net.bluemind.vertx.common;

import org.vertx.java.platform.Verticle;

import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.vertx.common.bus.CoreAuth;

public final class CoreAuthVerticleFactory implements IVerticleFactory {

	@Override
	public boolean isWorker() {
		return true;
	}

	@Override
	public Verticle newInstance() {
		return new CoreAuth();
	}

}
