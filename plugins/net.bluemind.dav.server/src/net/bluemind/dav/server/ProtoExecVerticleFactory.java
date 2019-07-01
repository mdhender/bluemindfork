package net.bluemind.dav.server;

import org.vertx.java.platform.Verticle;

import net.bluemind.lib.vertx.IVerticleFactory;

public final class ProtoExecVerticleFactory implements IVerticleFactory {

	@Override
	public boolean isWorker() {
		return true;
	}

	@Override
	public Verticle newInstance() {
		return new ProtocolExecutorVerticle();
	}

}
