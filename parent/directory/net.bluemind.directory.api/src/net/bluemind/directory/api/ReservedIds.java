package net.bluemind.directory.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReservedIds {

	public static class PreAllocatedId {
		public final String key;
		public final long id;

		@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
		public PreAllocatedId(@JsonProperty("key") String key, @JsonProperty("id") long id) {
			this.key = key;
			this.id = id;
		}

		@Override
		public String toString() {
			return "{" + key + ": " + id + "}";
		}
	}

	@FunctionalInterface
	public static interface ConsumerHandler {
		void acceptConsumer(Consumer<ReservedIds> dependencies);
	}

	public final Collection<PreAllocatedId> deps;

	public ReservedIds() {
		this.deps = new ArrayList<>();
	}

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public ReservedIds(@JsonProperty("deps") Collection<PreAllocatedId> deps) {
		this.deps = deps;
	}

	public void add(String folderKey, long allocatedId) {
		deps.add(new PreAllocatedId(folderKey, allocatedId));
	}

	public void forEach(Consumer<PreAllocatedId> action) {
		deps.forEach(action);
	}

	@Override
	public String toString() {
		return "ReservedIds{" + deps + "}";
	}

}
