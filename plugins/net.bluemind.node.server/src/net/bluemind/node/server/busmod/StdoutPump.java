/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.node.server.busmod;

import java.io.InputStream;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.vertx.core.json.JsonObject;
import net.bluemind.node.server.busmod.OutputSplitter.Line;
import net.bluemind.node.server.busmod.OutputSplitter.SplitException;
import net.bluemind.node.server.busmod.SysCommand.WsEndpoint;

public final class StdoutPump implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(StdoutPump.class);

	private final InputStream in;
	private final RunningCommand rc;
	private final OutputSplitter rp;
	private final Process proc;
	private final boolean recordOutput;
	private WsEndpoint wsEndpoint;

	public StdoutPump(Process proc, RunningCommand rc, boolean recordOutput, WsEndpoint wsEP) {
		this.proc = proc;
		this.in = proc.getInputStream();
		this.rc = rc;
		this.recordOutput = recordOutput;
		this.rp = new OutputSplitter(this::newLine);
		this.wsEndpoint = wsEP;
	}

	private void newLine(Line l) {
		String line = l.log;
		logger.debug("[{}]: {}", rc.getPid(), line);
		if (wsEndpoint != null) {
			JsonObject log = new JsonObject().put("log", line).put("continued", l.continued);
			wsEndpoint.write("log", log);
		} else {
			rc.out(line);
		}
	}

	public void run() {
		long time = System.currentTimeMillis();
		long count = 0;
		try {
			Integer exit = null;
			// process smaller than OutputSplitter#DEFAULT_MAX_FRAME_SIZE
			byte[] buf = new byte[4096];
			while (true) {
				int read = in.read(buf);
				if (read == -1) {
					break;
				}
				if (recordOutput && read > 0) {
					rp.write(Unpooled.wrappedBuffer(buf, 0, read));
				}
				logger.debug("[{}] pumped {}bytes.", rc.getPid(), read);
				count++;
			}
			logger.debug("Exited stream pump after {}loops.", count);
			exit = proc.waitFor();
			rp.end();
			notifyEndOnWs(exit);
			rc.setExitValue(exit, time);
			logger.info("[{}] exit: {} (loops: {})", rc.getPid(), exit, count);
			proc.destroy();
		} catch (SplitException se) {
			// this one does not flush the content on purpose
			logger.error("[{}] process will be destroyed because it gave non-utf8 output {}", rc.getPid(),
					se.getMessage());
			proc.destroyForcibly();
			notifyEndOnWs(1);
			rc.setExitValue(1, time);
		} catch (Exception e) {
			logger.error("[{}] {}", rc.getPid(), e.getMessage());
			proc.destroyForcibly();
			rp.end();
			notifyEndOnWs(1);
			rc.setExitValue(1, time);
		}
	}

	private void notifyEndOnWs(Integer exit) {
		if (wsEndpoint != null) {
			wsEndpoint.write("completion", new JsonObject().put("exit", Optional.ofNullable(exit).orElse(1)));
			wsEndpoint.complete(rc.getPid());
		}
	}
}
