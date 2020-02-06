package net.bluemind.dav.server;

import io.vertx.core.Verticle;
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
