package net.bluemind.metrics.registry.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.netflix.spectator.api.BasicTag;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Tag;

public class InfluxMetric {

	public final String meterType;
	public final Map<String, Long> values;
	public final String name;
	public final List<Tag> tags;

	public InfluxMetric(String name, List<Tag> tags, Map<String, Long> values, String meterType) {
		this.values = values;
		this.meterType = meterType;
		this.tags = tags;
		this.name = name;
	}

	public static InfluxMetric fromLineProtocol(String line) {

		String[] tokens = line.split(" ");

		String[] nameAndTags = tokens[0].split(",");
		List<Tag> tags = new ArrayList<Tag>();
		String meterType = "";

		for (int i = 1; i < nameAndTags.length; i++) {
			String[] keyVal = nameAndTags[i].split("=");
			if ("meterType".equals(keyVal[0])) {
				meterType = keyVal[1];
			} else {
				tags.add(new BasicTag(keyVal[0], keyVal[1]));
			}
		}

		String[] valuesTab = tokens[1].split(",");
		Map<String, Long> values = new HashMap<String, Long>();
		for (int i = 0; i < valuesTab.length; i++) {
			String[] keyVal = valuesTab[i].split("=");
			values.put(keyVal[0], Long.parseLong(keyVal[1]));
		}
		return new InfluxMetric(nameAndTags[0], tags, values, meterType);
	}

	public Id asId(Registry reg) {
		return reg.createId(name, tags);
	}
}
