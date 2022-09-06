package net.bluemind.startup.dropins;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Manifest;

import net.bluemind.startup.dropins.DropinsActivator.DropedJar;

public class ManifestVersionUpdater {

	public static Set<DropedJar> updateDropinJars(String productVersion, Path productPath) throws IOException {
		Path dropinsPath = productPath.resolve("extensions/eclipse/plugins");
		Set<DropedJar> dropedJars = new HashSet<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dropinsPath)) {
			stream.forEach(path -> updateJar(productVersion, path) //
					.map(name -> new DropedJar(productPath.relativize(path), name)) //
					.ifPresent(dropedJars::add));
		}
		return dropedJars;
	}

	private static Optional<String> updateJar(String productVersion, Path jarPath) {
		try (FileSystem jarFS = FileSystems.newFileSystem(URI.create("jar:" + jarPath.toUri()), Map.of())) {
			Path manifestPath = jarFS.getPath("META-INF", "MANIFEST.MF");
			Manifest manifest = readManifest(manifestPath);
			String bundleName = manifest.getMainAttributes().getValue("Bundle-Name");
			manifest.getMainAttributes().putValue("Bundle-Version", productVersion);
			writeManifest(manifestPath, manifest);
			return Optional.of(bundleName);
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
