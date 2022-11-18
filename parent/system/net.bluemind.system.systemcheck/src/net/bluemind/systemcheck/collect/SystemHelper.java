/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.systemcheck.collect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.task.service.IServerTaskMonitor;

public class SystemHelper {

	private static final Logger logger = LoggerFactory.getLogger(SystemHelper.class);
	private static final boolean debugMode = new File(System.getProperty("user.home") + "/ui.debug").exists();

	protected static final String BM_CONF = "/etc/bm";

	private SystemHelper() {
	}

	public static boolean isDebugMode() {
		return debugMode;
	}

	public static int cmd(String cmd, IServerTaskMonitor task) throws IOException {
		return cmd(cmd, task, null);
	}

	public static CmdOutput cmdWithEnv(String cmd, Map<String, String> customEnv) throws IOException {
		logger.info("--- {} ---", cmd);
		ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
		pb.redirectErrorStream(true);
		if (customEnv != null) {
			pb.environment().putAll(customEnv);
		}
		Process pid = pb.start();
		InputStream in = pid.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line = null;

		CmdOutput out = new CmdOutput();
		do {
			line = br.readLine();
			if (line != null) {
				out.out(line);
			}
			logger.info(line != null ? line : "---");
		} while (line != null);
		int exit = 1;
		try {
			exit = pid.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error("cmd: '{}' interrupted", cmd);
		}
		logger.info("cmd: '{}' exited ({})", cmd, exit);
		return out.code(exit);
	}

	public static int cmd(String cmd, IServerTaskMonitor task, Map<String, String> customEnv) throws IOException {
		logger.info("--- {} ---", cmd);
		ProcessBuilder pb = new ProcessBuilder(cmd.split(" "));
		pb.redirectErrorStream(true);
		if (customEnv != null) {
			pb.environment().putAll(customEnv);
		}
		Process pid = pb.start();
		InputStream in = pid.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line = null;

		if (task != null) {
			do {
				line = br.readLine();
				if (line != null) {
					task.log(line);
				}
				logger.info(line != null ? line : "---");
			} while (line != null);
		}
		int exit = 1;
		try {
			exit = pid.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error("cmd: '{}' interrupted", cmd);
		}
		logger.info("cmd: '{}' exited ({})", cmd, exit);
		return exit;
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static void emulate(IServerTaskMonitor monitor) {
		monitor.begin(3, "Starting create emulation....");
		monitor.progress(1, "Sleeping for 2secs");
		sleep(2000);
		monitor.progress(1, "#progress 66");
		monitor.progress(1, "Emulation complete :-)");
		sleep(1500);
	}

	public static void transfer(InputStream in, OutputStream out) throws IOException {
		final byte[] buffer = new byte[1024];

		try {
			while (true) {
				int amountRead = in.read(buffer);
				if (amountRead == -1) {
					break;
				}
				out.write(buffer, 0, amountRead);
			}
		} finally {
			in.close();
			out.flush();
			out.close();
		}
	}

	public static void configureBM(InputStream in) throws IOException {
		File bmConf = new File(BM_CONF + "/bm.ini");
		transfer(in, new FileOutputStream(bmConf));
	}

	public static boolean languageIsValid(String lang) {
		return !Strings.isNullOrEmpty(lang) && Stream.of(Locale.getAvailableLocales())
				.anyMatch(l -> l.getLanguage().equals(new Locale(lang).getLanguage()));
	}
}
