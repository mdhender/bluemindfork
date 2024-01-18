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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;

import io.netty.buffer.ByteBuf;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.lib.vertx.ContextNetSocket;
import net.bluemind.lib.vertx.VertxContext;
import net.bluemind.lib.vertx.utils.PasswordDecoder;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.ysnp.AuthConfig;
import net.bluemind.ysnp.YSNPConfiguration;

public class SaslAuthdVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(SaslAuthdVerticle.class);
	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory(MetricsRegistry.get(), SaslAuthdVerticle.class);
	private static final byte[] SASL_OK = new byte[] { 0, 2, (byte) 'O', (byte) 'K' };
	private static final byte[] SASL_FAILED = new byte[] { 0, 2, (byte) 'N', (byte) 'O' };

	private Supplier<Optional<String>> defaultDomain;

	private final String socketPath;
	private final AuthConfig authConfig;

	private static final ValidationPolicy POLICY = new ValidationPolicy(YSNPConfiguration.INSTANCE);

	public SaslAuthdVerticle(String socketPath) {
		this(socketPath, AuthConfig.defaultConfig());
	}

	public SaslAuthdVerticle(String socketPath, AuthConfig authConfig) {
		this.authConfig = authConfig;
		this.socketPath = socketPath;
	}

	@Override
	public void start(Promise<Void> startPromise) {
		AtomicReference<SharedMap<String, String>> sysconf = new AtomicReference<>();
		MQ.init().thenAccept(v -> sysconf.set(MQ.sharedMap("system.configuration")));

		defaultDomain = () -> Optional.ofNullable(sysconf.get())
				.map(sm -> Optional.ofNullable(sm.get(SysConfKeys.default_domain.name()) != null
						&& !sm.get(SysConfKeys.default_domain.name()).isEmpty()
								? sm.get(SysConfKeys.default_domain.name())
								: null))
				.orElse(Optional.empty());

		vertx.createNetServer().connectHandler(netsock -> {
			Context ctx = VertxContext.getOrCreateDuplicatedContext();
			ctx.runOnContext(v -> handleNetSock(new ContextNetSocket(ctx, netsock), POLICY));
		}).listen(SocketAddress.domainSocketAddress(socketPath), res -> {
			if (res.failed()) {
				logger.error(res.cause().getMessage(), res.cause());
				startPromise.fail(res.cause());
			} else {
				logger.info("Listening on {}", socketPath);
				startPromise.complete();
			}
		});
	}

	protected void handleNetSock(NetSocket netsock, ValidationPolicy vp) {
		netsock.exceptionHandler(t -> logger.error(t.getMessage(), t));
		netsock.handler(buf -> {
			Creds creds = parse(buf.getByteBuf());
			Timer timer = registry.timer(idFactory.name("validationTime"));
			long time = registry.clock().monotonicTime();
			vertx.executeBlocking(() -> {
				boolean valid = vp.validate(creds.login, creds.password, creds.service, creds.realm, authConfig);
				return valid;
			}).andThen(res -> {
				long elapsed = registry.clock().monotonicTime() - time;
				timer.record(elapsed, TimeUnit.NANOSECONDS);
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
		String password = PasswordDecoder.getPassword(login, v);

		v = new byte[buf.readShort()];
		buf.readBytes(v);
		String service = new String(v);

		v = new byte[buf.readShort()];
		buf.readBytes(v);
		String realm = new String(v);

		if (!"admin0".equals(login) && realm.isEmpty() && defaultDomain.get().isPresent()) {
			realm = defaultDomain.get().get();
		}
		return new Creds(login, password, service, realm);
	}

}
