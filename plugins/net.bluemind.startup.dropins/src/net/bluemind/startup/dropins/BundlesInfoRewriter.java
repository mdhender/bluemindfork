package net.bluemind.startup.dropins;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.function.Predicate;
import java.util.stream.Stream;

import net.bluemind.startup.dropins.Repository.Jar;

public class BundlesInfoRewriter {

	private record BundleInfo(String symbolicName, String version, String location, int startLevel,
			boolean markedAsStarted) {

		public BundleInfo withNewLocation(String newLocation) {
			return new BundleInfo(symbolicName, version, newLocation, startLevel, markedAsStarted);
		}

		public String toString() {
			return "%s,%s,%s,%d,%b".formatted(symbolicName, version, location, startLevel, markedAsStarted);
		}

		public static BundleInfo fromLine(String line) {
			String[] tokens = line.split(",");
			if (tokens.length < 5) {
				throw new IllegalArgumentException("Line does not contain at least 5 tokens: " + line);
			}
			return new BundleInfo(tokens[0], tokens[1], tokens[2], Integer.parseInt(tokens[3]),
					Boolean.parseBoolean(tokens[4]));
		}

		public static BundleInfo fromJar(Jar jar) {
			return new BundleInfo(jar.bundleName(), jar.bundleVersion(), jar.relativePath().toString(), 4, false);
		}

		public Jar toJar() {
			return new Jar(Paths.get(location), symbolicName, version);
		}
	}

	public static synchronized void rewriteBundlesInfo(Path bundlesInfoPath, Repository extensions, Repository dropins)
			throws IOException {
		Path originalBundlesInfoPath = bundlesInfoPath.getParent().resolve("bundles.info.installed");
		if (!Files.exists(originalBundlesInfoPath)) {
			Files.copy(bundlesInfoPath, originalBundlesInfoPath);
		}

		try {
			Path tmpBundlesInfoPath = Files.createTempFile("bundles.info", ".tmp");
			try (OutputStream out = Files.newOutputStream(tmpBundlesInfoPath, StandardOpenOption.CREATE,
					StandardOpenOption.SYNC, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
					Stream<String> lines = Files.lines(originalBundlesInfoPath)) {
				boolean failed = rewritePlugins(out, lines, extensions, dropins) //
						|| writeJar(out, extensions, extension -> !dropins.contains(extension)) //
						|| writeJar(out, dropins, dropin -> true);
				if (!failed) {
					Files.move(tmpBundlesInfoPath, bundlesInfoPath, StandardCopyOption.REPLACE_EXISTING);
				} else {
					Files.delete(tmpBundlesInfoPath);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean rewritePlugins(OutputStream fw, Stream<String> bundlesInfolines, Repository extensions,
			Repository dropins) {
		return bundlesInfolines //
				.filter(line -> line.length() != 0 && !line.startsWith("#")) //
				.map(BundleInfo::fromLine) //
				.map(bundleInfo -> updateBundleInfoLocation(bundleInfo, dropins)) //
				.map(bundleInfo -> {
					extensions.remove(bundleInfo.toJar());
					dropins.remove(bundleInfo.toJar());
					return writeBundleInfoLine(bundleInfo, fw);
				}) //
				.anyMatch(Boolean.FALSE::equals);
	}

	private static BundleInfo updateBundleInfoLocation(BundleInfo bundleInfo, Repository dropins) {
		return dropins.stream() //
				.filter(dropin -> dropin.bundleName().equals(bundleInfo.symbolicName)
						&& bundleInfo.symbolicName.startsWith("net.bluemind")) //
				.findFirst() //
				.map(dropin -> bundleInfo.withNewLocation(dropin.relativePath().toString())) //
				.orElse(bundleInfo);
	}

	private static boolean writeJar(OutputStream fw, Repository repo, Predicate<Jar> keep) {
		return repo.stream() //
				.filter(keep::test) //
				.map(BundleInfo::fromJar) //
				.map(bundleInfo -> writeBundleInfoLine(bundleInfo, fw)) //
				.anyMatch(Boolean.FALSE::equals);
	}

	private static boolean writeBundleInfoLine(BundleInfo bundleInfo, OutputStream fw) {
		try {
			String l = bundleInfo.toString() + "\n";
			fw.write(l.getBytes());
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}
