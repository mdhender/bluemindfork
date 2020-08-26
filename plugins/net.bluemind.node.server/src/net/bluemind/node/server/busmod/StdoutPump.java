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

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.parsetools.RecordParser;
import net.bluemind.node.server.busmod.SysCommand.WsEndpoint;

public final class StdoutPump implements Runnable {

	private static final byte[] LF = "\n".getBytes();
	private static final Logger logger = LoggerFactory.getLogger(StdoutPump.class);

	private final InputStream in;
	private final RunningCommand rc;
	private final RecordParser rp;
	private final Process proc;
	private final boolean record;
	private WsEndpoint wsEndpoint;

	public StdoutPump(Process proc, RunningCommand rc, boolean recordOutput, WsEndpoint wsEP) {
		this.proc = proc;
		this.in = proc.getInputStream();
		this.rc = rc;
		this.record = recordOutput;
		this.rp = RecordParser.newDelimited(Buffer.buffer(LF), new Handler<Buffer>() {

			@Override
			public void handle(Buffer event) {
				newLine(event);
			}
		});
		this.wsEndpoint = wsEP;
	}

	private void newLine(Buffer b) {
		String line = b.toString();
		logger.debug("[{}]: {}", rc.getPid(), line);
		rc.out(line);
		if (wsEndpoint != null) {
			JsonObject log = new JsonObject().put("log", line);
			wsEndpoint.write("log", log);
		}
	}

	private final byte[] chunk(byte[] buf, int len) {
		if (len == buf.length) {
			return buf;
		}
		byte[] result = new byte[len];
		System.arraycopy(buf, 0, result, 0, len);
		return result;
	}

	public void run() {
		long time = System.currentTimeMillis();
		long count = 0;
		try {
			Integer exit = null;
			byte[] buf = new byte[32768];
			while (true) {
				int read = in.read(buf);
				if (read == -1) {
					break;
				}
				if (record && read > 0) {
					Buffer chunk = Buffer.buffer(chunk(buf, read));
					rp.handle(chunk);
				}
				logger.debug("[{}] pumped {}bytes.", rc.getPid(), read);
				count++;
			}
			logger.debug("Exited stream pump after {}loops.", count);
			exit = proc.waitFor();
			notifyEndOnWs(exit);
			rc.setExitValue(exit, time);
			logger.info("[{}] exit: {} (loops: {})", rc.getPid(), exit, count);
			proc.destroy();
		} catch (Exception e) {
			rp.handle(Buffer.buffer(e.getMessage()));
			logger.error("[{}] {}", rc.getPid(), e.getMessage());
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
