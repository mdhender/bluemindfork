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
package net.bluemind.imap;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SearchQuery {

	private boolean unseenOnly;
	private Date after;
	private Date before;
	private String from;
	private String to;
	private String subject;
	private String body;
	private boolean useOr;
	private Integer rangeMin;
	private Integer rangeMax;
	private HashMap<String, String> headers;
	private String keyword;

	public SearchQuery() {
		this(null);
	}

	/**
	 * 
	 * @param after Messages whose internal date (disregarding time and timezone) is
	 *              within or later than the specified date.
	 */
	public SearchQuery(Date after) {
		this.after = after;
		useOr = false;
		rangeMin = null;
		rangeMax = null;
		this.headers = new HashMap<>();
	}

	public Date getAfter() {
		return after;
	}

	public void setAfter(Date after) {
		this.after = after;
	}

	public boolean isUnseenOnly() {
		return unseenOnly;
	}

	public void setUnseenOnly(boolean unseenOnly) {
		this.unseenOnly = unseenOnly;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public boolean isUseOr() {
		return useOr;
	}

	public void setUseOr(boolean useOr) {
		this.useOr = useOr;
	}

	public Integer getRangeMin() {
		return rangeMin;
	}

	public void setRangeMin(Integer rangeMin) {
		this.rangeMin = rangeMin;
	}

	public Integer getRangeMax() {
		return rangeMax;
	}

	public void setRangeMax(Integer rangeMax) {
		this.rangeMax = rangeMax;
	}

	public Date getBefore() {
		return before;
	}

	public void setBefore(Date before) {
		this.before = before;
	}

	public void headerMatch(String header, String value) {
		headers.put(header, value);
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * to retrieve messages with a specified keyword flag set
	 * 
	 * @param keyword
	 */
	public String getKeyword() {
		return keyword;
	}

	/**
	 * to retrieve messages with a specified keyword flag set
	 * 
	 * @param keyword
	 */
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

}
