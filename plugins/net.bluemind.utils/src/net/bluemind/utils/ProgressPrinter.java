/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.utils;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ProgressPrinter {
	private final long total;
	private long start;
	private long elements = 0L;
	private final long printEvery;
	private final long printEverySeconds;
	private Instant lastPrint = Instant.now();

	public ProgressPrinter(long total) {
		this(total, 10_000L, 2);
	}

	/**
	 * Generate a "progress" log every "printEvery" item, or every printEverySecond
	 * seconds
	 * 
	 * @param total             long: total number of exepected elements
	 * @param printEvery:       print the log every "n" elements
	 * @param printEverySeconds
	 */
	public ProgressPrinter(long total, long printEvery, long printEverySeconds) {
		this.total = total;
		this.printEvery = printEvery;
		this.printEverySeconds = printEverySeconds;
		this.start = System.nanoTime();
	}

	public void reset() {
		start = System.nanoTime();
	}

	public void add() {
		add(1L);
	}

	public void add(long elements) {
		this.elements += elements;
	}

	private String plural(long v, String s) {
		return v + " " + s + (v > 1 ? "s" : "");
	}

	private String formatDuration(Duration duration) {
		List<String> parts = new ArrayList<>();
		long days = duration.toDaysPart();
		if (days > 0) {
			parts.add(plural(days, "day"));
		}
		int hours = duration.toHoursPart();
		if (hours > 0 || !parts.isEmpty()) {
			parts.add(plural(hours, "hour"));
		}
		int minutes = duration.toMinutesPart();
		if (minutes > 0 || !parts.isEmpty()) {
			parts.add(plural(minutes, "minute"));
		}
		int seconds = duration.toSecondsPart();
		parts.add(plural(seconds, "second"));
		return String.join(", ", parts);
	}

	public String toString() {
		lastPrint = Instant.now();
		var duration = Duration.ofNanos(System.nanoTime() - start);
		var durationSeconds = duration.toSeconds();
		var percentage = total > 0 ? ((total - elements) / (double) total) : 100;
		StringBuilder sb = new StringBuilder();
		sb.append(elements).append(" / ").append(total);
		sb.append(" (").append(new DecimalFormat("#.0#").format(percentage)).append("%)");
		if (durationSeconds > 0) {
			var rate = elements / durationSeconds;
			sb.append(" in ").append(formatDuration(duration));
			sb.append(" rate: ").append(rate).append("/s");
			sb.append(" eta: ").append(formatDuration(Duration.ofSeconds((total - elements) / rate)));
		}

		return sb.toString();
	}

	public boolean shouldPrint() {
		return ((elements % printEvery) == 0) || elements == total
				|| Duration.between(lastPrint, Instant.now()).toSeconds() >= printEverySeconds;
	}
}