package net.bluemind.central.reverse.proxy.model.common.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class InstallationTopics {
	public String installationId;
	public String orphans;
	public Map<String, String> domainTopics = new HashMap<>();
	public String crpTopicName;
	public boolean hasCrpTopic = false;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public InstallationTopics(@JsonProperty("installationId") String installationId, //
			@JsonProperty("orphans") String orphans, //
			@JsonProperty("domainTopics") Map<String, String> domainTopics, //
			@JsonProperty("crpTopicName") String crpTopicName, //
			@JsonProperty("hasCrpTopic") boolean hasCrpTopic) {
		this.installationId = installationId;
		this.orphans = orphans;
		this.domainTopics = domainTopics;
		this.crpTopicName = crpTopicName;
		this.hasCrpTopic = hasCrpTopic;
	}

	public InstallationTopics(Set<String> topicNames, String crpTopicSuffix) {
		installationId = topicNames.stream() //
				.map(topicName -> {
					String[] tokens = topicName.split("-");
					return (tokens.length > 1) ? tokens[0] : null;
				}).filter(Objects::nonNull).findFirst() //
				.orElseThrow(() -> new RuntimeException("No installation id deduced from topic names"));

		crpTopicName = installationId + "-" + crpTopicSuffix;
		topicNames.stream().filter(topicName -> topicName.startsWith(installationId)).forEach(topicName -> {
			if (topicName.endsWith("__orphans__")) {
				this.orphans = topicName;
			} else if (topicName.equals(crpTopicName)) {
				this.hasCrpTopic = true;
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
