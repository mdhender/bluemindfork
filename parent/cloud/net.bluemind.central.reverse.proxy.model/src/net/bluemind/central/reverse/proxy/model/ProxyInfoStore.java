package net.bluemind.central.reverse.proxy.model;

import static net.bluemind.central.reverse.proxy.model.common.ProxyInfoStoreEventBusAddress.ADDRESS;
import static net.bluemind.central.reverse.proxy.model.common.ProxyInfoStoreEventBusAddress.ADD_DIR_NAME;
import static net.bluemind.central.reverse.proxy.model.common.ProxyInfoStoreEventBusAddress.ADD_DOMAIN_NAME;
import static net.bluemind.central.reverse.proxy.model.common.ProxyInfoStoreEventBusAddress.ADD_INSTALLATION_NAME;
import static net.bluemind.central.reverse.proxy.model.common.ProxyInfoStoreEventBusAddress.ALL_IPS_NAME;
import static net.bluemind.central.reverse.proxy.model.common.ProxyInfoStoreEventBusAddress.ANY_IP_NAME;
import static net.bluemind.central.reverse.proxy.model.common.ProxyInfoStoreEventBusAddress.HEADER_ACTION;
import static net.bluemind.central.reverse.proxy.model.common.ProxyInfoStoreEventBusAddress.HEADER_TS;
import static net.bluemind.central.reverse.proxy.model.common.ProxyInfoStoreEventBusAddress.IP_NAME;
import static net.bluemind.central.reverse.proxy.model.common.ProxyInfoStoreEventBusAddress.TIME_MANAGE_WARN;
import static net.bluemind.central.reverse.proxy.model.common.ProxyInfoStoreEventBusAddress.TIME_PROCES_WARN;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.curator.shaded.com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.model.common.DirInfo;
import net.bluemind.central.reverse.proxy.model.common.DirInfo.DirEmail;
import net.bluemind.central.reverse.proxy.model.common.DomainInfo;
import net.bluemind.central.reverse.proxy.model.common.InstallationInfo;

public class ProxyInfoStore {
	private final Logger logger = LoggerFactory.getLogger(ProxyInfoStore.class);

	private final ProxyInfoStorage storage;

	private MessageConsumer<JsonObject> consumer;

	private ProxyInfoStore(ProxyInfoStorage storage) {
		this.storage = storage;
	}

	public static ProxyInfoStore create() {
		return ProxyInfoStore.create(ProxyInfoStorage.create());
	}

	public static ProxyInfoStore create(ProxyInfoStorage storage) {
		return new ProxyInfoStore(storage);
	}

	public ProxyInfoStore setupService(Vertx vertx) {
		consumer = vertx.eventBus().<JsonObject>consumer(ADDRESS).handler(event -> {
			logEventProcessDuration(event);
			long time = System.currentTimeMillis();

			String action = event.headers().get(HEADER_ACTION);
			switch (action) {
			case ADD_DIR_NAME:
				addDir(event);
				break;
			case ADD_DOMAIN_NAME:
				addDomain(event);
				break;
			case ADD_INSTALLATION_NAME:
				addInstallation(event);
				break;
			case IP_NAME:
				ip(event);
				break;
			case ANY_IP_NAME:
				anyIp(event);
				break;
			case ALL_IPS_NAME:
				allIps(event);
				break;
			default:
				event.fail(404, "Unknown action '" + action + "'");
			}

			time = System.currentTimeMillis() - time;
			if (logger.isDebugEnabled()) {
				logger.debug("ProxyInfoStore: vertx event management took {}ms long", time);
			} else if (time > TIME_MANAGE_WARN) {
				logger.warn("ProxyInfoStore: vertx event management took more than {}ms long: {}ms", TIME_MANAGE_WARN,
						time);
			}
		});

		return this;
	}

	private void logEventProcessDuration(Message<JsonObject> event) {
		try {
			long ts = Long.parseLong(event.headers().get(HEADER_TS));

			long processTime = System.currentTimeMillis() - ts;
			if (processTime > TIME_PROCES_WARN) {
				logger.warn("ProxyInfoStore: vertx event process took more than {}ms long: {}ms", TIME_PROCES_WARN, ts);
			}
		} catch (NumberFormatException nfe) {
			// Ignore bad event timestamp
		}
	}

	private void addDir(Message<JsonObject> event) {
		try {
			DirInfo dir = event.body().mapTo(DirInfo.class);

			if (dir.kind == null || !dir.kind.equalsIgnoreCase("user") || dir.emails.isEmpty()) {
				event.reply(null);
			}

			dir.emails.stream() //
					.flatMap(email -> expand(email, dir.domainUid).stream()) //
					.forEach(email -> storage.addLogin(email, dir.dataLocation));
			event.reply(null);
		} catch (IllegalArgumentException e) {
			failToDecodeParams(event);
		}
	}

	private void failToDecodeParams(Message<JsonObject> event) {
		event.fail(500, "unable to decode parameters '" + event.body().encode() + "'");
	}

	private List<String> expand(DirEmail email, String domainUid) {
		if (email.allAliases) {
			Set<String> domainAliases = storage.domainAliases(domainUid);
			domainAliases.add(domainUid);
			String leftPart = email.address.split("@")[0];
			return domainAliases.stream().map(alias -> leftPart + "@" + alias).toList();
		} else {
			return Collections.singletonList(email.address);
		}
	}

	private void addDomain(Message<JsonObject> event) {
		try {
			DomainInfo domain = event.body().mapTo(DomainInfo.class);
			storage.addDomain(domain.uid, domain.aliases);
			event.reply(null);
		} catch (IllegalArgumentException e) {
			failToDecodeParams(event);
		}
	}

	private void addInstallation(Message<JsonObject> event) {
		try {
			InstallationInfo installation = event.body().mapTo(InstallationInfo.class);
			if (!installation.hasNginx) {
				event.reply(null);
			}

			String oldIp = storage.addDataLocation(installation.dataLocationUid, installation.ip);
			event.reply(oldIp);
		} catch (IllegalArgumentException e) {
			failToDecodeParams(event);
		}
	}

	private void ip(Message<JsonObject> event) {
		String login = event.body().getString("login");
		if (Objects.isNull(login)) {
			failToDecodeParams(event);
		} else {
			String ip = storage.ip(login);
			if (!Objects.isNull(ip)) {
				event.reply(new JsonObject().put("ip", ip));
			} else {
				event.fail(404, "No IP found for '" + login + "'");
			}
		}
	}

	private void anyIp(Message<JsonObject> event) {
		String anyIp = storage.anyIp();
		if (!Objects.isNull(anyIp)) {
			event.reply(new JsonObject().put("ip", anyIp));
		} else {
			event.fail(404, "No IPs found");
		}
	}

	private void allIps(Message<JsonObject> event) {
		event.reply(new JsonObject().put("ips", storage.allIps()));
	}

	@VisibleForTesting
	public void tearDown() throws InterruptedException, ExecutionException {
		if (consumer != null) {
			CompletableFuture<Void> c = new CompletableFuture<>();
			consumer.unregister().onSuccess(c::complete).onFailure(c::completeExceptionally);
			c.get();
		}
	}

}
