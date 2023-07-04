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
package net.bluemind.imap.command;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import net.bluemind.imap.SearchQuery;

public class UIDSearchCommand extends AbstractUIDSearchCommand {

	// Mon, 7 Feb 1994 21:52:25 -0800

	private SearchQuery sq;

	public UIDSearchCommand(SearchQuery sq) {
		this.sq = sq;
	}

	@Override
	protected CommandArgument buildCommand() {
		String prefix = "UID SEARCH";
		String cmd = "";
		if (sq.isUnseenOnly()) {
			cmd += " UNSEEN";
		}
		if (sq.isAll()) {
			cmd += " ALL";
		}
		if (sq.isNotDeleted()) {
			cmd += " NOT DELETED";
		}
		if (sq.isNotSeen()) {
			cmd += " NOT SEEN";
		}
		if (sq.isSeen()) {
			cmd += " SEEN";
		}
		if (sq.isNotUnseen()) {
			cmd += " NOT UNSEEN";
		}
		if (sq.isAnswered()) {
			cmd += " ANSWERED";
		}
		if (sq.isNotAnswered()) {
			cmd += " NOT ANSWERED";
		}
		if (sq.isUnanswered()) {
			cmd += " UNANSWERED";
		}
		if (sq.isNotUnanswered()) {
			cmd += " NOT UNANSWERED";
		}
		if (sq.isFlagged()) {
			cmd += " FLAGGED";
		}
		if (sq.isNotFlagged()) {
			cmd += " NOT FLAGGED";
		}
		if (sq.isUnflagged()) {
			cmd += " UNFLAGGED";
		}
		if (sq.isNotUnflagged()) {
			cmd += " NOT UNFLAGGED";
		}
		if (sq.isDraft()) {
			cmd += " DRAFT";
		}
		if (sq.isNotDraft()) {
			cmd += " NOT DRAFT";
		}
		if (sq.isUndraft()) {
			cmd += " UNDRAFT";
		}
		if (sq.isNotUndraft()) {
			cmd += " NOT UNDRAFT";
		}
		if (sq.isUseOr()) {
			cmd += " OR";
		}
		if (sq.getTo() != null) {
			cmd += " TO \"" + sq.getTo() + "\"";
		}
		if (sq.getNotTo() != null) {
			cmd += " NOT TO \"" + sq.getNotTo() + "\"";
		}
		if (sq.getFrom() != null) {
			cmd += " FROM \"" + sq.getFrom() + "\"";
		}
		if (sq.getNotFrom() != null) {
			cmd += " NOT FROM \"" + sq.getNotFrom() + "\"";
		}
		if (sq.getBcc() != null) {
			cmd += " BCC \"" + sq.getBcc() + "\"";
		}
		if (sq.getNotBcc() != null) {
			cmd += " NOT BCC \"" + sq.getNotBcc() + "\"";
		}
		if (sq.getSubject() != null) {
			cmd += " SUBJECT \"" + sq.getSubject() + "\"";
		}
		if (sq.getNotSubject() != null) {
			cmd += " NOT SUBJECT \"" + sq.getNotSubject() + "\"";
		}
		if (sq.getCc() != null) {
			cmd += " CC \"" + sq.getCc() + "\"";
		}
		if (sq.getNotCc() != null) {
			cmd += " NOT CC \"" + sq.getNotCc() + "\"";
		}
		if (sq.getBody() != null) {
			cmd += " BODY \"" + sq.getBody() + "\"";
		}
		if (sq.getNotBody() != null) {
			cmd += " NOT BODY \"" + sq.getNotBody() + "\"";
		}
		if (sq.getText() != null) {
			cmd += " TEXT \"" + sq.getText() + "\"";
		}
		if (sq.getNotText() != null) {
			cmd += " NOT TEXT \"" + sq.getNotText() + "\"";
		}
		if (sq.getAfter() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " SINCE " + df.format(sq.getAfter());
		}
		if (sq.getNotAfter() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " NOT SINCE " + df.format(sq.getNotAfter());
		}
		if (sq.getBefore() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " BEFORE " + df.format(sq.getBefore());
		}
		if (sq.getNotBefore() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " NOT BEFORE " + df.format(sq.getNotBefore());
		}
		if (sq.getSentBefore() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " SENTBEFORE " + df.format(sq.getSentBefore());
		}
		if (sq.getNotSentBefore() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " NOT SENTBEFORE " + df.format(sq.getNotSentBefore());
		}
		if (sq.getSentSince() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " SENTSINCE " + df.format(sq.getSentSince());
		}
		if (sq.getNotSentSince() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " NOT SENTSINCE " + df.format(sq.getNotSentSince());
		}
		if (sq.getOn() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " ON " + df.format(sq.getOn());
		}
		if (sq.getNotOn() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " NOT ON " + df.format(sq.getNotOn());
		}
		if (sq.getSentOn() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " SENTON " + df.format(sq.getSentOn());
		}
		if (sq.getNotSentOn() != null) {
			DateFormat df = new SimpleDateFormat("d-MMM-yyyy", Locale.ENGLISH);
			cmd += " NOT SENTON " + df.format(sq.getNotSentOn());
		}
		if (sq.getKeyword() != null) {
			cmd += " KEYWORD " + sq.getKeyword();
		}
		if (sq.getLarger() != null) {
			cmd += " LARGER " + sq.getLarger();
		}
		if (sq.getNotLarger() != null) {
			cmd += " NOT LARGER " + sq.getNotLarger();
		}
		if (sq.getSmaller() != null) {
			cmd += " SMALLER " + sq.getSmaller();
		}
		if (sq.getNotSmaller() != null) {
			cmd += " NOT SMALLER " + sq.getNotSmaller();
		}
		if (sq.getUidSeq() != null) {
			cmd += " UID " + sq.getUidSeq();
		}
		if (sq.getNotUidSeq() != null) {
			cmd += " NOT UID " + sq.getNotUidSeq();
		}
		if (sq.getSeq() != null) {
			cmd += " " + sq.getSeq();
		}

		Map<String, String> heads = sq.getHeaders();
		if (!heads.isEmpty()) {
			for (Entry<String, String> s : heads.entrySet()) {
				cmd += " HEADER " + s.getKey() + " \"" + s.getValue() + "\"";
			}
		}

		Map<String, String> notHeads = sq.getNotHeaders();
		if (!notHeads.isEmpty()) {
			for (Entry<String, String> s : notHeads.entrySet()) {
				cmd += " NOT HEADER " + s.getKey() + " \"" + s.getValue() + "\"";
			}
		}

		if (sq.getAfterOr() != null && sq.getBeforeOr() != null) {
			cmd += " OR " + sq.getBeforeOr() + " " + sq.getAfterOr();
		}
		if (cmd.isBlank()) {
			cmd = " ALL";
		}
		cmd = prefix + cmd;

		if (sq.getRawCommand() != null) {
			cmd = "UID SEARCH " + sq.getRawCommand();
		}

		return new CommandArgument(cmd, null);
	}

}
