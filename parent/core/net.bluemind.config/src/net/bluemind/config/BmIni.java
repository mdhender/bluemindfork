package net.bluemind.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BmIni {
	private static final Logger logger = LoggerFactory.getLogger(BmIni.class);

	private static final String BM_INI = "/etc/bm/bm.ini";
	private static final File iniFile = new File(BM_INI);

	private BmIni() {
	}

	public static Map<String, String> get() {
		Properties props = new Properties();
		if (iniFile.exists()) {
			try (InputStream in = Files.newInputStream(iniFile.toPath())) {
				props.load(in);

			} catch (IOException e) {
				logger.error("error during loading {}", BM_INI, e);
			}
		} else {
			logger.warn("{} not found", BM_INI);
		}

		Map<String, String> values = new HashMap<>();

		for (Entry<Object, Object> entry : props.entrySet()) {
			values.put((String) entry.getKey(), (String) entry.getValue());
		}
		return values;
	}

	public static String getPgPassword() throws FileNotFoundException {
		if (!iniFile.exists()) {
			throw new FileNotFoundException(BM_INI);
		}
		Properties p = new Properties();
		try (InputStream in = Files.newInputStream(iniFile.toPath())) {
			p.load(in);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return p.getProperty("password").replace("\"", "");
	}

	public static String value(String key) {
		if (iniFile.exists()) {
			Properties p = new Properties();
			try (InputStream in = Files.newInputStream(iniFile.toPath())) {
				p.load(in);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			return p.getProperty(key);
		} else {
			return null;
		}
	}

}
