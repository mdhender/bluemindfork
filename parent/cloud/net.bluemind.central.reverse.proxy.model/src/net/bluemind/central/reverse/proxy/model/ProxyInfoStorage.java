package net.bluemind.central.reverse.proxy.model;

import java.util.List;
import java.util.Set;

import net.bluemind.central.reverse.proxy.model.impl.HashMapProxyInfoStorage;

public interface ProxyInfoStorage {

	static ProxyInfoStorage create() {
		return new HashMapProxyInfoStorage();
	}

	String addLogin(String login, String dataLocation);

	String addDataLocation(String dataLocation, String ip);

	Set<String> addDomain(String domainUid, Set<String> aliases);

	String ip(String login);

	List<String> allIps();

	String anyIp();

	Set<String> domainAliases(String domainUid);
}
