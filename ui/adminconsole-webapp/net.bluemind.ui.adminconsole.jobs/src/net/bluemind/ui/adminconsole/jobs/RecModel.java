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
package net.bluemind.ui.adminconsole.jobs;

public class RecModel {

	private String seconds;

	private String minutes;

	private String hours;

	private String daysOfMonth;

	private String month;

	private String daysOfWeek;

	private String year;

	public RecModel(String cronString) {
		String[] splitted = cronString.split(" ");
		int i = 0;
		seconds = splitted[i++];
		minutes = splitted[i++];
		hours = splitted[i++];
		daysOfMonth = splitted[i++];
		month = splitted[i++];
		daysOfWeek = splitted[i++];
		if (splitted.length == 7) {
			year = splitted[i++];
		} else {
			year = "*";
		}
	}

	public String getCronString() {
		StringBuilder sb = new StringBuilder(18);
		sb.append(seconds);
		sb.append(' ');
		sb.append(minutes);
		sb.append(' ');
		sb.append(hours);
		sb.append(' ');
		sb.append(daysOfMonth);
		sb.append(' ');
		sb.append(month);
		sb.append(' ');
		sb.append(daysOfWeek);
		sb.append(' ');
		sb.append(year);
		return sb.toString();
	}

	public String getSeconds() {
		return seconds;
	}

	public void setSeconds(String seconds) {
		this.seconds = seconds;
	}

	public String getMinutes() {
		return minutes;
	}

	public void setMinutes(String minutes) {
		this.minutes = minutes;
	}

	public String getHours() {
		return hours;
	}

	public void setHours(String hours) {
		this.hours = hours;
	}

	public String getDaysOfMonth() {
		return daysOfMonth;
	}

	public void setDaysOfMonth(String days) {
		this.daysOfMonth = days;
	}

	public String getMonth() {
		return month;
	}

	public void setMonth(String month) {
		this.month = month;
	}

	public String getDaysOfWeek() {
		return daysOfWeek;
	}

	public void setDaysOfWeek(String lastField) {
		this.daysOfWeek = lastField;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

}
