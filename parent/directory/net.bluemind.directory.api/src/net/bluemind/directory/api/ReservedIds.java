package net.bluemind.directory.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReservedIds {

	public static class CyrusId {
		public final String key;
		public final long id;

		@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
		public CyrusId(@JsonProperty("key") String key, @JsonProperty("id") long id) {
			this.key = key;
			this.id = id;
		}
	}

	@FunctionalInterface
	public static interface ConsumerHandler {
		void acceptConsumer(Consumer<ReservedIds> dependencies);
	}

	public final Collection<CyrusId> deps;

	public ReservedIds() {
		this.deps = new ArrayList<>();
	}

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public ReservedIds(@JsonProperty("deps") Collection<CyrusId> deps) {
		this.deps = deps;
	}

	public void add(String folderKey, long allocatedId) {
		deps.add(new CyrusId(folderKey, allocatedId));
	}

	public void forEach(Consumer<CyrusId> action) {
		deps.forEach(action);
	}

}
