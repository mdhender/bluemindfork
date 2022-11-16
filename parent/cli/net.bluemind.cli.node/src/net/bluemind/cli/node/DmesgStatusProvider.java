/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.cli.node;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.ProcessHandler;
import net.bluemind.node.shared.ExecRequest;
import net.bluemind.server.api.Server;

public class DmesgStatusProvider implements IStatusProvider {

	@Override
	public void report(CliContext ctx, ItemValue<Server> srv, INodeClient nc) {
		CountDownLatch cdl = new CountDownLatch(1);
		nc.asyncExecute(ExecRequest.anonymous("dmesg --time-format iso --level=err,warn"), new ProcessHandler() {

			private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			private final long now = System.currentTimeMillis();

			@Override
			public void starting(String taskRef) {
				// coucou
			}

			/**
			 * <code>
			 * 2022-06-30T15:47:06,013824+0200 INFO: task vert.x-eventloo:26621 blocked for more than 120 seconds.
			 * </code>
			 */
			@Override
			public void log(String l, boolean isContinued) {
				try {
					int commaInDateIdx = l.indexOf(',');
					if (commaInDateIdx == -1) {
						return;
					}
					String justTheDate = l.substring(0, commaInDateIdx);
					String log = l.substring(l.indexOf(' ') + 1);
					Date date = iso.parse(justTheDate);
					if (now - date.getTime() < TimeUnit.DAYS.toMillis(2)) {
						ctx.warn("DMESG {}: {}", date, log);
					}
				} catch (Exception e) {
					// ok
				}
			}

			@Override
			public void completed(int exitCode) {
				cdl.countDown();
			}
		});
		try {
			cdl.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
