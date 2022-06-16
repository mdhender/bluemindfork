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

	@Override
	public String addLogin(String login, String dataLocation) {
		return emailDataLocation.put(login, dataLocation);
	}

	@Override
	public String addDataLocation(String dataLocation, String ip) {
		return dataLocationDownstreamIp.put(dataLocation, ip);
	}

	@Override
	public Set<String> addDomain(String domainUid, Set<String> aliases) {
		return domainUidAliases.put(domainUid, aliases);
	}

	@Override
	public String ip(String login) {
		String dataLocation = emailDataLocation.get(login);
		return dataLocationDownstreamIp.get(dataLocation);
	}

	@Override
	public List<String> allIps() {
		return new ArrayList<>(dataLocationDownstreamIp.values());
	}

	@Override
	public String anyIp() {
		List<String> allIps = allIps();
		return !allIps.isEmpty() ? allIps.get(ThreadLocalRandom.current().nextInt(allIps.size())) : null;
	}

	@Override
	public Set<String> domainAliases(String domainUid) {
		return domainUidAliases.getOrDefault(domainUid, new HashSet<>());
	}
}
