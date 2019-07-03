/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.bmgroups;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.config.Token;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.domain.api.IDomainsPromise;
import net.bluemind.group.api.IGroupPromise;
import net.bluemind.network.topology.Topology;
import net.bluemind.user.api.IUserPromise;
import net.bluemind.user.api.User;

public class GroupProtocolHandler implements Handler<Buffer> {
	private static final Logger logger = LoggerFactory.getLogger(GroupProtocolHandler.class);
	private final HttpClientProvider clientProvider;
	private final NetSocket socket;
	private final Handler<Throwable> exceptionHandler;
	private final ILocator cachingLocator;
	private static Cache<String, ItemValue<User>> usersCache = CacheBuilder.newBuilder()
			.expireAfterAccess(10, TimeUnit.MINUTES).build();
	private static Cache<String, List<String>> memberOfCache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES).build();

	public static Cache<String, ItemValue<User>> getUsersCache() {
		return usersCache;
	}

	public static Cache<String, List<String>> getMemberOfCache() {
		return memberOfCache;
	}

	private static class UserAndGroups {
		public UserAndGroups(ItemValue<User> user, List<String> groups) {
			this.user = user;
			this.groups = groups;
		}

		ItemValue<User> user;
		List<String> groups;
	}

	public GroupProtocolHandler(HttpClientProvider clientProvider, NetSocket socket) {
		this.clientProvider = clientProvider;
		this.socket = socket;
		this.cachingLocator = (String service, AsyncHandler<String[]> asyncHandler) -> {
			String core = Topology.get().core().value.address();
			String[] resp = new String[] { core };
			asyncHandler.success(resp);
		};
		this.exceptionHandler = new Handler<Throwable>() {

			@Override
			public void handle(Throwable e) {
				logger.error("error: {}", e.getMessage(), e);
				socket.write(ko(String.format("error: %s", e.getMessage())));
				socket.close();
			}
		};
		socket.exceptionHandler(this.exceptionHandler);
	}

	@Override
	public void handle(Buffer event) {
		String loginAtDomain = event.toString().trim();
		loginAtDomain = loginAtDomain.replace('^', '.');
		logger.debug("search for login {}", loginAtDomain);
		if ("admin0".equals(loginAtDomain)) {
			socket.write(ok(null));
			socket.close();
			return;
		}

		if (loginAtDomain.indexOf("@") == -1) {
			logger.error("Invalid login: {}", loginAtDomain);
			socket.write(ko(String.format("Invalid login: %s", loginAtDomain)));
			socket.close();
			return;
		}
		String login = loginAtDomain.split("@")[0];
		String domain = loginAtDomain.split("@")[1];

		VertxPromiseServiceProvider provider = new VertxPromiseServiceProvider(clientProvider, cachingLocator,
				Token.admin0());

		if (login.startsWith("group:") && login.substring("group:".length()).equals(domain)) {

			provider.instance(IDomainsPromise.class).get(domain).thenAccept(value -> {
				if (value == null) {
					socket.write(ko(String.format("error: group (domain) %s not found", domain)));
					socket.close();
					return;
				}

				socket.write(ok(null));
				socket.close();
			}).exceptionally(t -> {
				exceptionHandler.handle(t);
				return null;
			});

		} else if (login.startsWith("group:")) {
			String uid = login.substring("group:".length());
			provider.instance(IGroupPromise.class, domain).getComplete(uid).thenAccept(value -> {
				if (value == null) {
					socket.write(ko(String.format("error: group %s not found", uid)));
					socket.close();
					return;
				}

				socket.write(ok(null));
				socket.close();
			}).exceptionally(t -> {
				exceptionHandler.handle(t);
				return null;
			});

		} else {

			long time = System.currentTimeMillis();

			ItemValue<User> userFromCache = usersCache.getIfPresent(domain + "-" + login);
			CompletableFuture<ItemValue<User>> userFuture = null;
			if (userFromCache != null) {
				userFuture = CompletableFuture.completedFuture(userFromCache);
			} else {
				userFuture = provider.instance(IUserPromise.class, domain).byLogin(login).thenCompose(user -> {
					if (user == null) {
						return provider.instance(IUserPromise.class, domain).getComplete(login);
					} else {
						return CompletableFuture.completedFuture(user);
					}
				});
			}
			userFuture.thenCompose(user -> {
				if (user == null) {
					throw new ServerFault("user " + login + "@" + domain + " not found", ErrorCode.NOT_FOUND);
				}

				if (userFromCache == null) {
					usersCache.put(domain + "-" + login, user);
				}
				List<String> memberOfs = memberOfCache.getIfPresent(domain + "-" + user.uid);
				if (memberOfs != null) {
					return CompletableFuture.completedFuture(new UserAndGroups(user, memberOfs));
				} else {
					return provider.instance(IUserPromise.class, domain).memberOfGroups(user.uid)

							.thenApply(g -> {
								memberOfCache.put(domain + "-" + user.uid, g);
								return new UserAndGroups(user, g);
							});
				}
			}).thenAccept(groupsAndUser -> {
				logger.trace("time to found user and memberof time {}", (System.currentTimeMillis() - time));
				List<String> ret = new ArrayList<>(groupsAndUser.groups.size());
				for (String g : groupsAndUser.groups) {
					ret.add("group:" + g + "@" + domain);
				}
				logger.debug("found user {}@{}, memberof {}", login, domain, ret);

				String res = String.join(",", ret);
				if (!res.isEmpty()) {
					res += ",";
				}
				// default groups (dmail + useruid)
				res += "group:" + domain + "@" + domain + "," + groupsAndUser.user.uid + "@" + domain;
				socket.write(ok(res));
				socket.close();
			}).exceptionally(t -> {
				exceptionHandler.handle(t);
				return null;
			});
		}

	}

	private Buffer ok(String msg) {
		logger.debug("ok {}", msg);
		Buffer message = new Buffer();
		message.appendString("OK");
		if (msg != null) {
			message.appendString(msg);
		}
		Buffer ret = new Buffer();
		ret.appendShort((short) message.length());
		ret.appendBuffer(message);
		logger.debug("return message {}", ret);
		return ret;
	}

	private Buffer ko(String msg) {
		logger.error("ko {}", msg);
		Buffer message = new Buffer();
		message.appendString("KO ").appendString(msg);
		Buffer ret = new Buffer().appendShort((short) message.length()).appendBuffer(message);
		logger.error("return message {}", ret);
		return ret;
	}

}
