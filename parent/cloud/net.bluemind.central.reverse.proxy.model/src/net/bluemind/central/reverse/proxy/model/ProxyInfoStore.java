package net.bluemind.central.reverse.proxy.model;

import static net.bluemind.central.reverse.proxy.model.ProxyInfoStoreAddress.ADDRESS;
import static net.bluemind.central.reverse.proxy.model.ProxyInfoStoreAddress.ADD_DIR_NAME;
import static net.bluemind.central.reverse.proxy.model.ProxyInfoStoreAddress.ADD_DOMAIN_NAME;
import static net.bluemind.central.reverse.proxy.model.ProxyInfoStoreAddress.ADD_INSTALLATION_NAME;
import static net.bluemind.central.reverse.proxy.model.ProxyInfoStoreAddress.ALL_IPS_NAME;
import static net.bluemind.central.reverse.proxy.model.ProxyInfoStoreAddress.ANY_IP_NAME;
import static net.bluemind.central.reverse.proxy.model.ProxyInfoStoreAddress.IP_NAME;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.model.mapper.DirInfo;
import net.bluemind.central.reverse.proxy.model.mapper.DirInfo.DirEmail;
import net.bluemind.central.reverse.proxy.model.mapper.DomainInfo;
import net.bluemind.central.reverse.proxy.model.mapper.InstallationInfo;

public class ProxyInfoStore {

	private final Vertx vertx;
	private final ProxyInfoStorage storage;

	private MessageConsumer<JsonObject> consumer;

	private ProxyInfoStore(Vertx vertx, ProxyInfoStorage storage) {
		this.vertx = vertx;
		this.storage = storage;
	}

	public static ProxyInfoStore create(Vertx vertx) {
		return ProxyInfoStore.create(vertx, ProxyInfoStorage.create());
	}

	public static ProxyInfoStore create(Vertx vertx, ProxyInfoStorage storage) {
		return new ProxyInfoStore(vertx, storage);
	}

	public void setup() {
		consumer = vertx.eventBus().<JsonObject>consumer(ADDRESS).handler(event -> {
			String action = event.headers().get("action");
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
		});
	}

	private void addDir(Message<JsonObject> event) {
		try {
			DirInfo dir = event.body().mapTo(DirInfo.class);
			dir.emails.stream().flatMap(email -> expand(email, dir.domainUid).stream())
					.forEach(email -> storage.addLogin(email, dir.dataLocation));
			event.reply(null);
		} catch (IllegalArgumentException e) {
			event.fail(500, "unable to decode parameters '" + event.body().encode() + "'");
		}
	}

	private List<String> expand(DirEmail email, String domainUid) {
		if (email.allAliases) {
			Set<String> domainAliases = storage.domainAliases(domainUid);
			domainAliases.add(domainUid);
			String leftPart = email.address.split("@")[0];
			return domainAliases.stream().map(alias -> leftPart + "@" + alias).collect(Collectors.toList());
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
			event.fail(500, "unable to decode parameters '" + event.body().encode() + "'");
		}
	}

	private void addInstallation(Message<JsonObject> event) {
		try {
			InstallationInfo installation = event.body().mapTo(InstallationInfo.class);
			storage.addDataLocation(installation.dataLocation, installation.ip);
			event.reply(null);
		} catch (IllegalArgumentException e) {
			event.fail(500, "unable to decode parameters '" + event.body().encode() + "'");
		}
	}

	private void ip(Message<JsonObject> event) {
		String login = event.body().getString("login");
		if (Objects.isNull(login)) {
			event.fail(500, "unable to decode parameters '" + event.body().encode() + "'");
		} else {
			String ip = storage.ip(login);
			if (!Objects.isNull(ip)) {
				event.reply(new JsonObject().put("ip", ip));
			} else {
				event.fail(404, "No IP found for'" + login + "'");
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

	public void tearDown() {
		if (consumer != null) {
			consumer.unregister();
		}
	}

}
