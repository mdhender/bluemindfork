package net.bluemind.webmodule.devmodefilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.webmodule.devmodefilter.DevModeState.ServerPort;

public class PortsForward extends AbstractVerticle {

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return false;
		}

		@Override
		public Verticle newInstance() {
			return new PortsForward();
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(PortsForward.class);
	private StateWatcher stateWatcher;
	private DevModeState state;
	private List<NetServer> servers = Collections.emptyList();

	@Override
	public void start() {
		stateWatcher = new StateWatcher(vertx, (state) -> {
			updateState(state);
			return null;
		});
		stateWatcher.start();
	}

	public void updateState(DevModeState newState) {
		if (state != null) {
			stopState();
		}

		state = newState;
		servers = new ArrayList<>(state.forwardPorts.size());

		state.forwardPorts.forEach(forwardPort -> {
			ServerPort server = state.servers.get(forwardPort.serverId);
			NetServer netServer = vertx.createNetServer().connectHandler(sock -> {
				sock.pause();
				logger.info("forward to {}:{}", server.ip, server.port);
				vertx.createNetClient().connect(server.port, server.ip, cSock -> {
					if (cSock.succeeded()) {
						sock.resume();
						biPump(cSock.result(), sock);
					} else {
						sock.close();
					}
				});
			}).listen(forwardPort.src);
			servers.add(netServer);
		});

	}

	protected void biPump(NetSocket a, NetSocket b) {
		a.pipe().endOnComplete(false).to(b, h -> {
			a.close();
			b.close();
		});
		b.pipe().endOnComplete(false).to(a, h -> {
			a.close();
			b.close();
		});
	}

	private void stopState() {
		servers.forEach(NetServer::close);
	}

}
