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
import java.util.concurrent.atomic.AtomicLong;

public class ProgressPrinter {
	private final long total;
	private long start;
	private AtomicLong elements = new AtomicLong(0);
	private final long printEvery;
	private final long printEverySeconds;
	private Instant lastPrint = Instant.now();
	private static final DecimalFormat DEC_FORMAT = new DecimalFormat("#.##");

	private static final long KB = 1L * 1000;
	private static final long MB = KB * 1000;
	private static final long GB = MB * 1000;
	private static final long TB = GB * 1000;
	private static final long PB = TB * 1000;
	private static final long EB = PB * 1000;

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
		elements.incrementAndGet();
	}

	public void add(long elements) {
		this.elements.addAndGet(elements);
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
		if (seconds < 0) {
			parts.add("immediately");
		} else {
			parts.add(plural(seconds, "second"));
		}
		return String.join(", ", parts);
	}

	public static String toHumanReadableSIPrefixes(long size) {
		if (size < 0)
			throw new IllegalArgumentException("Invalid file size: " + size);
		if (size >= EB)
			return formatSize(size, EB, "E");
		if (size >= PB)
			return formatSize(size, PB, "P");
		if (size >= TB)
			return formatSize(size, TB, "T");
		if (size >= GB)
			return formatSize(size, GB, "G");
		if (size >= MB)
			return formatSize(size, MB, "M");
		if (size >= KB)
			return formatSize(size, KB, "K");
		return formatSize(size, 1L, "");
	}

	private static String formatSize(long size, long divider, String unitName) {
		return DEC_FORMAT.format((double) size / divider) + " " + unitName;
	}

	public String toString() {
		try {
			lastPrint = Instant.now();
			var duration = Duration.ofNanos(System.nanoTime() - start);
			var currentElements = elements.get();
			var durationSeconds = duration.toSeconds();
			var percentage = total > 0 ? ((currentElements / (double) total) * 100.0) : 100.0;
			StringBuilder sb = new StringBuilder();
			sb.append(currentElements).append(" / ").append(total);
			sb.append(" (").append(new DecimalFormat("00.0#").format(percentage)).append("%)");
			if (durationSeconds > 0) {
				var rate = currentElements / durationSeconds;
				sb.append(" in ").append(formatDuration(duration));
				sb.append(" rate: ").append(toHumanReadableSIPrefixes(rate)).append("/s");
				if (rate > 0) {
					sb.append(" eta: ").append(formatDuration(Duration.ofSeconds((total - currentElements) / rate)));
				}
			}
			return sb.toString();
		} catch (Exception e) {
			return e.getMessage();
		}

	}

	public boolean shouldPrint() {
		var currentElements = elements.get();
		return ((currentElements % printEvery) == 0) || currentElements == total
				|| Duration.between(lastPrint, Instant.now()).toSeconds() >= printEverySeconds;
	}
}