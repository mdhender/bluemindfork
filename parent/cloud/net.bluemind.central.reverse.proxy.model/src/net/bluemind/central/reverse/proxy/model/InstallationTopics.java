package net.bluemind.central.reverse.proxy.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class InstallationTopics {
	public String orphans;
	public Map<String, String> domainTopics = new HashMap<>();

	public InstallationTopics(Set<String> topicNames) {
		String installationId = topicNames.stream().map(topicName -> {
			String[] tokens = topicName.split("-");
			return (tokens.length > 1) ? tokens[0] : null;
		}).filter(Objects::nonNull).findFirst()
				.orElseThrow(() -> new RuntimeException("No installation id deduced from topic names"));

		topicNames.stream().filter(topicName -> topicName.startsWith(installationId)).forEach(topicName -> {
			if (topicName.endsWith("__orphans__")) {
				this.orphans = topicName;
			} else {
				String[] tokens = topicName.split("-");
				if (tokens.length > 1) {
					this.domainTopics.put(tokens[1], topicName);
				}
			}
		});
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		return builder.append("InstallationTopics [orphans=").append(orphans).append(", domainTopics=")
				.append(domainTopics).append("]").toString();
	}

}
