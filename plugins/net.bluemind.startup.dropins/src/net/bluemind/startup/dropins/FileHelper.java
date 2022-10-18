package net.bluemind.startup.dropins;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class FileHelper {

	public static void deleteFolder(Path path) throws IOException {
		try (Stream<Path> walk = Files.walk(path)) {
			walk.sorted(Comparator.reverseOrder()).forEach(FileHelper::deleteFile);
		}
	}

	public static boolean createFolder(Path path) {
		return path.toFile().mkdirs();
	}

	public static void deleteFile(Path path) {
		try {
			Files.delete(path);
		} catch (IOException e) {
			System.err.printf("Unable to delete this path : %s%n%s", path, e);
		}
	}
}
