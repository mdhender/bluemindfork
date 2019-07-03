/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.adm;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.iq80.snappy.SnappyInputStream;

import com.google.common.io.ByteStreams;

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;

@SuppressWarnings("deprecation")
@Command(name = "hsm-to-cyrus", description = "Converts HSM snappy spool to a cyrus maildir folder")
public class HsmToCyrus implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("maintenance");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return HsmToCyrus.class;
		}
	}

	@Option(name = "--output-dir", description = "The directory where the output dir will be created", required = true)
	public String outputDir;

	@Option(name = "--uid", description = "The user uid to convert", required = true)
	public String uid;

	@Option(name = "--domain", description = "The domain of the uid", required = true)
	public String domain;

	private CliContext ctx;

	@Override
	public void run() {
		List<String> topDir = Arrays.asList("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e",
				"f");
		AtomicLong cpt = new AtomicLong();
		File tgt = new File(outputDir, "hsm-" + uid);
		tgt.mkdirs();
		for (String rootLvl : topDir) {
			for (String subLvl : topDir) {
				String sourceDir = String.format("/var/spool/bm-hsm/snappy/user/%s/%s/%s/%s", domain, uid, rootLvl,
						subLvl);
				File srcDir = new File(sourceDir);
				if (srcDir.exists() && srcDir.isDirectory())
					try (Stream<Path> dirStream = Files.list(srcDir.toPath())) {
						dirStream.forEach(p -> unpack(cpt.incrementAndGet(), tgt, p));
					} catch (IOException e) {
						ctx.error("Error streaming dir " + srcDir.getAbsolutePath());
					}
			}
		}
	}

	private void unpack(long l, File outputParent, Path snapPath) {
		File dest = new File(outputParent, l + ".");
		try (SnappyInputStream snap = new SnappyInputStream(Files.newInputStream(snapPath));
				OutputStream target = Files.newOutputStream(dest.toPath())) {
			ByteStreams.copy(snap, target);
		} catch (IOException e) {
			ctx.error("Invalid file " + snapPath.toAbsolutePath());
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
