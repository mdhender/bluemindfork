package net.bluemind.central.reverse.proxy.model.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import net.bluemind.central.reverse.proxy.model.ProxyInfoStorage;

public class HashMapProxyInfoStorage implements ProxyInfoStorage {

	private final Map<String, String> emailDataLocation = new HashMap<>();
	private final Map<String, String> dataLocationDownstreamIp = new HashMap<>();
	private final Map<String, Set<String>> domainUidAliases = new HashMap<>();

	public void addLogin(String login, String dataLocation) {
		emailDataLocation.put(login, dataLocation);
	}

	public void addDataLocation(String dataLocation, String ip) {
		dataLocationDownstreamIp.put(dataLocation, ip);
	}

	public void addDomain(String domainUid, Set<String> aliases) {
		domainUidAliases.put(domainUid, aliases);
	}

	public String ip(String login) {
		String dataLocation = emailDataLocation.get(login);
		return dataLocationDownstreamIp.get(dataLocation);
	}

	public List<String> allIps() {
		return new ArrayList<>(dataLocationDownstreamIp.values());
	}

	public String anyIp() {
		List<String> allIps = allIps();
		return !allIps.isEmpty() ? allIps.get(ThreadLocalRandom.current().nextInt(allIps.size())) : null;
	}

	public Set<String> domainAliases(String domainUid) {
		return domainUidAliases.getOrDefault(domainUid, new HashSet<>());
	}
}
