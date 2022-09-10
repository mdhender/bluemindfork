package net.bluemind.startup.dropins;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.bluemind.startup.dropins.Repository.Jar;

public class JarManager {

	public static Set<Jar> listJars(Path productPath, Path jarsPath) throws IOException {
		return listJars(productPath, jarsPath, null);
	}

	public static Set<Jar> listJars(Path productPath, Path jarsPath, String forcedVersion) throws IOException {
		try (Stream<Path> stream = Files.walk(jarsPath)) {
			return stream //
					.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".jar")) //
					.map(path -> toJar(productPath, path, forcedVersion)) //
					.filter(Optional::isPresent).map(Optional::get) //
					.collect(Collectors.toSet());
		}
	}

	private static Optional<Jar> toJar(Path productPath, Path jarPath, String forcedVersion) {
		try (FileSystem jarFS = FileSystems.newFileSystem(URI.create("jar:" + jarPath.toUri()), Map.of())) {
			Path manifestPath = jarFS.getPath("META-INF", "MANIFEST.MF");
			Manifest manifest = readManifest(manifestPath);
			String bundleName = manifest.getMainAttributes().getValue("Bundle-Name");
			String bundleVersion;
			if (forcedVersion != null) {
				manifest.getMainAttributes().putValue("Bundle-Version", forcedVersion);
				writeManifest(manifestPath, manifest);
				bundleVersion = forcedVersion;
			} else {
				bundleVersion = manifest.getMainAttributes().getValue("Bundle-Version");
			}
			return Optional.ofNullable(new Jar(productPath.relativize(jarPath), bundleName, bundleVersion));
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	private static Manifest readManifest(Path manifestPath) throws IOException {
		try (InputStream is = Files.newInputStream(manifestPath)) {
			return new Manifest(is);
		}
	}

	private static void writeManifest(Path manifestPath, Manifest manifest) throws IOException {
		try (OutputStream os = Files.newOutputStream(manifestPath)) {
			manifest.write(os);
		}
	}

}
