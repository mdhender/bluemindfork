package net.bluemind.startup.dropins;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import net.bluemind.startup.dropins.Repository.Jar;

public record Repository(Set<Jar> jars) {

	public static record Jar(Path relativePath, String bundleName, String bundleVersion) {

		@Override
		public int hashCode() {
			return Objects.hash(bundleName);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Jar other = (Jar) obj;
			return Objects.equals(bundleName, other.bundleName);
		}
	}

	public Stream<Jar> stream() {
		return jars.stream();
	}

	public boolean remove(Jar jar) {
		return jars.remove(jar);
	}

	public boolean contains(Jar jar) {
		return jars.contains(jar);
	}

	public static Repository create(Path productPath, String subdirectory, String productVersion) throws IOException {
		Path repositoryPath = productPath.resolve(subdirectory);
		Set<Jar> jars = JarManager.listJars(productPath, repositoryPath, productVersion);
		return new Repository(jars);
	}
}
