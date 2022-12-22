package net.bluemind.lib.elasticsearch.allocations;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SimulationStatLoader {

	private static final Logger logger = LoggerFactory.getLogger(SimulationStatLoader.class);

	public static List<AllocationShardStats> marseille() {
		return load("marseille");
	}

	public static List<AllocationShardStats> laforet() {
		return load("laforet");
	}

	public static List<AllocationShardStats> load(String dataset) {
		Map<String, AllocationShardStats> stats = new HashMap<>();
		// curl -XGET 'http://localhost:9200/mailspool_*/_stats/' | \
		// jq -r '.indices | to_entries | map({index: .key, count:
		// .value.total.docs.count, deleted: .value.total.docs.deleted, refresh_count:
		// .value.total.refresh.external_total, refresh_duration:
		// .value.total.refresh.external_total_time_in_millis})' >
		// dataset-mailspool-stats.json
		try (InputStream jsonInput = SimulationStatLoader.class.getClassLoader()
				.getResourceAsStream("data/" + dataset + "-mailspool-stats.json")) {
			JsonNode node = new ObjectMapper().readTree(jsonInput);
			for (int i = 0; i < node.size(); i++) {
				JsonNode element = node.get(i);
				AllocationShardStats stat = new AllocationShardStats();
				stat.indexName = element.get("index").asText();
				stat.docCount = element.get("count").asLong();
				stat.deletedCount = element.get("deleted").asLong();
				stat.externalRefreshCount = element.get("refresh_count").asLong();
				stat.externalRefreshDuration = element.get("refresh_duration").asLong();
				stats.put(stat.indexName, stat);
			}
		} catch (IOException e) {
			fail(e.getMessage());
		}
		// curl -XGET 'http://localhost:9200/mailspool_*/_alias/' | \
		// jq -r 'to_entries | map({index: .key, count: .value.aliases | to_entries |
		// length})' > dataset-mailspool-aliases-count.json
		try (InputStream jsonInput = SimulationStatLoader.class.getClassLoader()
				.getResourceAsStream("data/" + dataset + "-mailspool-aliases-count.json")) {
			JsonNode node = new ObjectMapper().readTree(jsonInput);
			for (int i = 0; i < node.size(); i++) {
				JsonNode element = node.get(i);
				AllocationShardStats stat = stats.get(element.get("index").asText());
				stat.mailboxes = mailboxes(element.get("index").asText(), element.get("count").asInt());
			}
		} catch (IOException e) {
			fail(e.getMessage());
		}
		return new ArrayList<>(stats.values());
	}

	private static Set<String> mailboxes(String name, int boxCount) {
		return IntStream.range(0, boxCount).mapToObj(i -> name + "_" + i).collect(Collectors.toSet());
	}

}
