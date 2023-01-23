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

	private boolean all;
	private boolean seen;
	private boolean notSeen;
	private boolean unseenOnly;
	private boolean notUnseen;
	private boolean answered;
	private boolean notAnswered;
	private boolean unanswered;
	private boolean notUnanswered;
	private boolean deleted;
	private boolean notDeleted;
	private boolean undeleted;
	private boolean notUndeleted;
	private boolean draft;
	private boolean notDraft;
	private boolean undraft;
	private boolean notUndraft;
	private boolean flagged;
	private boolean notFlagged;
	private boolean unflagged;
	private boolean notUnflagged;
	private Date after;
	private Date notAfter;
	private Date sentSince;
	private Date notSentSince;
	private Date before;
	private Date notBefore;
	private Date sentBefore;
	private Date notSentBefore;
	private Date on;
	private Date notOn;
	private Date sentOn;
	private Date notSentOn;
	private String from;
	private String notFrom;
	private String to;
	private String notTo;
	private String cc;
	private String notCc;
	private String bcc;
	private String notBcc;
	private String subject;
	private String notSubject;
	private String body;
	private String notBody;
	private String text;
	private String notText;
	private boolean useOr;
	private Integer rangeMin;
	private Integer rangeMax;
	private HashMap<String, String> headers;
	private HashMap<String, String> notHeaders;
	private String keyword;
	private String notKeyword;
	private String unkeyword;
	private String notUnKeyword;
	private Integer larger;
	private Integer notLarger;
	private Integer smaller;
	private Integer notSmaller;
	private String seq;
	private String uidSeq;
	private String notUidSeq;
	private String beforeOr;
	private String afterOr;
	private String rawCommand;

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
		this.notHeaders = new HashMap<>();
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

	public void headerNotMatch(String header, String value) {
		notHeaders.put(header, value);
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

	public void setSeen(boolean s) {
		this.seen = s;
	}

	public boolean isSeen() {
		return this.seen;
	}

	public void setAnswered(boolean a) {
		this.answered = a;
	}

	public boolean isAnswered() {
		return this.answered;
	}

	public void setUnanswered(boolean u) {
		this.unanswered = u;
	}

	public boolean isUnanswered() {
		return this.unanswered;
	}

	public void setDeleted(boolean d) {
		this.deleted = d;
	}

	public boolean isDeleted() {
		return this.deleted;
	}

	public void setUndeleted(boolean u) {
		this.undeleted = u;
	}

	public boolean isUndeleted() {
		return this.undeleted;
	}

	public void setDraft(boolean d) {
		this.draft = d;
	}

	public boolean isDraft() {
		return this.draft;
	}

	public void setUndraft(boolean u) {
		this.undraft = u;
	}

	public boolean isUndraft() {
		return this.undraft;
	}

	public void setFlagged(boolean f) {
		this.flagged = f;
	}

	public boolean isFlagged() {
		return this.flagged;
	}

	public void setUnflagged(boolean u) {
		this.unflagged = u;
	}

	public boolean isUnflagged() {
		return this.unflagged;
	}

	public void setLarger(Integer l) {
		this.larger = l;
	}

	public Integer getLarger() {
		return this.larger;
	}

	public void setSmaller(Integer s) {
		this.smaller = s;
	}

	public Integer getSmaller() {
		return this.smaller;
	}

	public boolean isNotSeen() {
		return notSeen;
	}

	public void setNotSeen(boolean notSeen) {
		this.notSeen = notSeen;
	}

	public boolean isNotAnswered() {
		return notAnswered;
	}

	public void setNotAnswered(boolean notAnswered) {
		this.notAnswered = notAnswered;
	}

	public boolean isNotUnanswered() {
		return notUnanswered;
	}

	public void setNotUnanswered(boolean notUnanswered) {
		this.notUnanswered = notUnanswered;
	}

	public boolean isNotDeleted() {
		return notDeleted;
	}

	public void setNotDeleted(boolean notDeleted) {
		this.notDeleted = notDeleted;
	}

	public boolean isNotUndeleted() {
		return notUndeleted;
	}

	public void setNotUndeleted(boolean notUndeleted) {
		this.notUndeleted = notUndeleted;
	}

	public boolean isNotDraft() {
		return notDraft;
	}

	public void setNotDraft(boolean notDraft) {
		this.notDraft = notDraft;
	}

	public boolean isNotUndraft() {
		return notUndraft;
	}

	public void setNotUndraft(boolean notUndraft) {
		this.notUndraft = notUndraft;
	}

	public boolean isNotFlagged() {
		return notFlagged;
	}

	public void setNotFlagged(boolean notFlagged) {
		this.notFlagged = notFlagged;
	}

	public boolean isNotUnflagged() {
		return notUnflagged;
	}

	public void setNotUnflagged(boolean notUnflagged) {
		this.notUnflagged = notUnflagged;
	}

	public Date getNotAfter() {
		return notAfter;
	}

	public void setNotAfter(Date notAfter) {
		this.notAfter = notAfter;
	}

	public Date getNotBefore() {
		return notBefore;
	}

	public void setNotBefore(Date notBefore) {
		this.notBefore = notBefore;
	}

	public String getNotFrom() {
		return notFrom;
	}

	public void setNotFrom(String notFrom) {
		this.notFrom = notFrom;
	}

	public String getNotTo() {
		return notTo;
	}

	public void setNotTo(String notTo) {
		this.notTo = notTo;
	}

	public String getNotBody() {
		return notBody;
	}

	public void setNotBody(String notBody) {
		this.notBody = notBody;
	}

	public String getNotSubject() {
		return notSubject;
	}

	public void setNotSubject(String notSubject) {
		this.notSubject = notSubject;
	}

	public HashMap<String, String> getNotHeaders() {
		return notHeaders;
	}

	public void setNotHeaders(HashMap<String, String> notHeaders) {
		this.notHeaders = notHeaders;
	}

	public String getNotKeyword() {
		return notKeyword;
	}

	public void setNotKeyword(String notKeyword) {
		this.notKeyword = notKeyword;
	}

	public Integer getNotLarger() {
		return notLarger;
	}

	public void setNotLarger(Integer notLarger) {
		this.notLarger = notLarger;
	}

	public Integer getNotSmaller() {
		return notSmaller;
	}

	public void setNotSmaller(Integer notSmaller) {
		this.notSmaller = notSmaller;
	}

	public boolean isNotUnseen() {
		return notUnseen;
	}

	public void setNotUnseen(boolean notUnseen) {
		this.notUnseen = notUnseen;
	}

	public String getCc() {
		return cc;
	}

	public void setCc(String cc) {
		this.cc = cc;
	}

	public String getNotCc() {
		return notCc;
	}

	public void setNotCc(String notCc) {
		this.notCc = notCc;
	}

	public String getBcc() {
		return bcc;
	}

	public void setBcc(String bcc) {
		this.bcc = bcc;
	}

	public String getNotBcc() {
		return notBcc;
	}

	public void setNotBcc(String notBcc) {
		this.notBcc = notBcc;
	}

	public Date getSentBefore() {
		return sentBefore;
	}

	public void setSentBefore(Date sentBefore) {
		this.sentBefore = sentBefore;
	}

	public Date getNotSentBefore() {
		return notSentBefore;
	}

	public void setNotSentBefore(Date notSentBefore) {
		this.notSentBefore = notSentBefore;
	}

	public Date getSentSince() {
		return sentSince;
	}

	public void setSentSince(Date sentSince) {
		this.sentSince = sentSince;
	}

	public Date getNotSentSince() {
		return notSentSince;
	}

	public void setNotSentSince(Date notSentSince) {
		this.notSentSince = notSentSince;
	}

	public String getUidSeq() {
		return uidSeq;
	}

	public void setUidSeq(String uidSeq) {
		this.uidSeq = uidSeq;
	}

	public void setOr(String f, String s) {
		this.beforeOr = f;
		this.afterOr = s;
	}

	public String getBeforeOr() {
		return beforeOr;
	}

	public String getAfterOr() {
		return afterOr;
	}

	public Date getOn() {
		return on;
	}

	public void setOn(Date on) {
		this.on = on;
	}

	public Date getNotOn() {
		return notOn;
	}

	public void setNotOn(Date notOn) {
		this.notOn = notOn;
	}

	public Date getSentOn() {
		return sentOn;
	}

	public void setSentOn(Date sentOn) {
		this.sentOn = sentOn;
	}

	public Date getNotSentOn() {
		return notSentOn;
	}

	public void setNotSentOn(Date notSentOn) {
		this.notSentOn = notSentOn;
	}

	public String getUnkeyword() {
		return unkeyword;
	}

	public void setUnkeyword(String unkeyword) {
		this.unkeyword = unkeyword;
	}

	public String getNotUnKeyword() {
		return notUnKeyword;
	}

	public void setNotUnKeyword(String notUnKeyword) {
		this.notUnKeyword = notUnKeyword;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getNotText() {
		return notText;
	}

	public void setNotText(String notText) {
		this.notText = notText;
	}

	public boolean isAll() {
		return all;
	}

	public void setAll(boolean all) {
		this.all = all;
	}

	public String getSeq() {
		return seq;
	}

	public void setSeq(String seq) {
		this.seq = seq;
	}

	public String getRawCommand() {
		return rawCommand;
	}

	public void setRawCommand(String rawCommand) {
		this.rawCommand = rawCommand;
	}

	public String getNotUidSeq() {
		return notUidSeq;
	}

	public void setNotUidSeq(String notUidSeq) {
		this.notUidSeq = notUidSeq;
	}

}
