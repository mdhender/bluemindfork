/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.cli.utils;

import org.fusesource.jansi.Ansi;

import net.bluemind.core.task.service.IServerTaskMonitor;

public class CliTaskMonitor implements IServerTaskMonitor {
	private final String logPrefix;

	public CliTaskMonitor() {
		this("");
	}

	public CliTaskMonitor(String logPrefix) {
		this.logPrefix = logPrefix;
	}

	private String pprint(String log) {
		return logPrefix.isEmpty() ? log : "[" + logPrefix + "] " + log;
	}

	protected Ansi ansi() {
		return Ansi.ansi();
	}

	@Override
	public IServerTaskMonitor subWork(double work) {
		return this;
	}

	@Override
	public IServerTaskMonitor subWork(String logPrefix, double work) {
		return new CliTaskMonitor(this.logPrefix.isEmpty() ? logPrefix : this.logPrefix + " - " + logPrefix);
	}

	@Override
	public void begin(double totalWork, String log) {
		if (log != null) {
			System.err.println(ansi().fgCyan().a(pprint(log)).reset()); // NOSONAR
		}
	}

	@Override
	public void progress(double doneWork, String log) {
		if (log != null) {
			System.err.println(ansi().fgGreen().a(pprint(log)).reset()); // NOSONAR
		}
	}

	public void progress(int total, int current) {
		System.err.println(ansi().fgGreen() // NOSONAR
				.a(String.format("Global progress %d/%d (%s%%)", current, total, current * 100 / total)).reset());
	}

	@Override
	public void end(boolean success, String log, String result) {
		Ansi color = success ? ansi().fgGreen() : ansi().fgRed();
		if (log != null) {
			System.err.print(color.a(pprint(log)).reset()); // NOSONAR
		} else {
			System.err.print(color.a(pprint(success ? "task ended successfully" : "task failed")).reset()); // NOSONAR
		}
		System.err.println(result != null ? ": " + result : ""); // NOSONAR
	}

	@Override
	public void log(String log) {
		System.err.println(pprint(log)); // NOSONAR
	}
}
