/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.ysnp.impl;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;

import io.netty.buffer.ByteBuf;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.ysnp.YSNPConfiguration;

public class SaslAuthdVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(SaslAuthdVerticle.class);
	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory(MetricsRegistry.get(), SaslAuthdVerticle.class);
	private static final byte[] SASL_OK = new byte[] { 0, 2, (byte) 'O', (byte) 'K' };
	private static final byte[] SASL_FAILED = new byte[] { 0, 2, (byte) 'N', (byte) 'O' };

	private Supplier<String> defaultDomain;

	private final String socketPath;
	private final boolean expireOk;

	private static final ValidationPolicy POLICY = new ValidationPolicy(YSNPConfiguration.INSTANCE);

	public SaslAuthdVerticle(String socketPath, boolean expireOk) {
		this.expireOk = expireOk;
		this.socketPath = socketPath;
	}

	@Override
	public void start() {

		AtomicReference<SharedMap<String, String>> sysconf = new AtomicReference<>();
		MQ.init().thenAccept(v -> {
			sysconf.set(MQ.sharedMap("system.configuration"));
		});

		defaultDomain = () -> Optional.ofNullable(sysconf.get()).map(sm -> sm.get("default-domain")).orElse(null);

		NetServerOptions nso = new NetServerOptions().setTcpNoDelay(true);
		NetServer ns = vertx.createNetServer(nso);

		ns.connectHandler(netsock -> handleNetSock(netsock, POLICY));
		SocketAddress sock = SocketAddress.domainSocketAddress(socketPath);
		ns.listen(sock, res -> {
			if (res.failed()) {
				logger.error(res.cause().getMessage(), res.cause());
			}
		});
	}

	protected void handleNetSock(NetSocket netsock, ValidationPolicy vp) {
		netsock.exceptionHandler(t -> logger.error(t.getMessage(), t));
		netsock.handler(buf -> {
			Creds creds = parse(buf.getByteBuf());
			vertx.executeBlocking((Promise<Boolean> p) -> {
				try {
					boolean valid = vp.validate(creds.login, creds.password, creds.service, creds.realm, expireOk);
					p.complete(valid);
				} catch (Exception e) {
					p.fail(e);
				}
			}, res -> {
				if (res.succeeded() && res.result().booleanValue()) {
					registry.counter(idFactory.name("authCount", "status", "ok", "service", creds.service)).increment();
					netsock.write(Buffer.buffer(SASL_OK));
				} else {
					registry.counter(idFactory.name("authCount", "status", "failed", "service", creds.service))
							.increment();
					netsock.write(Buffer.buffer(SASL_FAILED));
				}
			});
		});
	}

	private static class Creds {
		String login;
		String password;
		String service;
		String realm;

		public Creds(String login, String password, String service, String realm) {
			this.login = login;
			this.password = password;
			this.service = service;
			this.realm = realm;
		}

	}

	private Creds parse(ByteBuf buf) {
		byte[] v = new byte[buf.readShort()];
		buf.readBytes(v);
		String login = new String(v);

		v = new byte[buf.readShort()];
		buf.readBytes(v);
		String password = new String(v);

		v = new byte[buf.readShort()];
		buf.readBytes(v);
		String service = new String(v);

		v = new byte[buf.readShort()];
		buf.readBytes(v);
		String realm = new String(v);

		if (!"admin0".equals(login) && realm.isEmpty() && defaultDomain.get() != null) {
			realm = defaultDomain.get();
		}
		return new Creds(login, password, service, realm);
	}
}
