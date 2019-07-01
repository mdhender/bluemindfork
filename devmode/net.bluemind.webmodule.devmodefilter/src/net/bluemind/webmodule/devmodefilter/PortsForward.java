package net.bluemind.webmodule.devmodefilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.net.NetServer;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.core.streams.Pump;
import org.vertx.java.platform.Verticle;

import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.webmodule.devmodefilter.DevModeState.ServerPort;

public class PortsForward extends Verticle {

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
		Pump p1 = Pump.createPump(a, b);
		Pump p2 = Pump.createPump(b, a);
		a.closeHandler(v -> b.close());
		b.closeHandler(v -> a.close());
		a.exceptionHandler(v -> a.close());
		b.exceptionHandler(v -> b.close());
		p1.start();
		p2.start();
	}

	private void stopState() {
		servers.forEach((s) -> s.close());
	}

}
