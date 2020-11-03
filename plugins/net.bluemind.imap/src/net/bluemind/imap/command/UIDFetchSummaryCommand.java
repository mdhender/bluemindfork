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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPHeaders;
import net.bluemind.imap.InternalDate;
import net.bluemind.imap.Summary;
import net.bluemind.imap.command.parser.FlagsStringParser;
import net.bluemind.imap.command.parser.HeadersParser;
import net.bluemind.imap.impl.DecoderUtils;
import net.bluemind.imap.impl.IMAPResponse;

public final class UIDFetchSummaryCommand extends Command<Collection<Summary>> {

	private static final String iDateStart = "INTERNALDATE \"";
	private static final int iDSLength = iDateStart.length();
	private static final String RCH;

	static {
		String[] headers = { "DATE", "FROM", "TO", "CC", "SUBJECT", "CONTENT-TYPE", "REPLY-TO", "LIST-POST",
				"DISPOSITION-NOTIFICATION-TO", "X-PRIORITY", "X-BM_HSM_ID", "X-BM_HSM_DATETIME", "X-BM-EVENT",
				"X-BM-RESOURCEBOOKING", "X-BM-FOLDERSHARING", "X-ASTERISK-CALLERID", "X-BM-EVENT-COUNTERED" };
		StringBuilder sb = new StringBuilder(1024);
		sb.append("BODY.PEEK[HEADER.FIELDS (");
		for (int i = 0; i < headers.length; i++) {
			if (i > 0) {
				sb.append(" ");
			}
			sb.append(headers[i]);
		}
		sb.append(")]");
		RCH = sb.toString();
	}

	private String uidSet;

	public UIDFetchSummaryCommand(String uidSet) {
		this.uidSet = uidSet;
	}

	@Override
	protected CommandArgument buildCommand() {
		// command length is 235 when uidSet is 1:*
		StringBuilder sb = new StringBuilder(256 + uidSet.length());
		sb.append("UID FETCH ");
		sb.append(uidSet).append(" (");
		sb.append("UID INTERNALDATE RFC822.SIZE FLAGS ");
		// roundcube message list headers
		sb.append(RCH).append(")");

		String cmd = sb.toString();
		logger.debug("[{}] cmd: {}", sb.length(), cmd);
		CommandArgument args = new CommandArgument(cmd, null);
		return args;
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		final int rsSize = rs.size();
		IMAPResponse last = rs.get(rsSize - 1);
		data = new ArrayList<>(rsSize);
		if (last.isOk()) {
			Iterator<IMAPResponse> it = rs.iterator();
			for (int i = 0; i < rsSize - 1; i++) {
				IMAPResponse r = it.next();

				Summary summary = parseSummary(r);
				if (summary != null) {
					data.add(summary);
				}
			}
		} else {
			logger.warn("error on fetch summary: " + last.getPayload());
		}
	}

	private Summary parseSummary(IMAPResponse r) {
		String payload = r.getPayload();
		logger.debug("payload: {}", payload);

		int fidx = payload.indexOf(iDateStart);
		if (fidx == -1) {
			logger.warn("Skipping parsing on {}", payload);
			return null;
		}
		fidx += iDSLength;

		int endDate = payload.indexOf('"', fidx);
		Date asDate = null;
		String internalDate = "";
		if (fidx > 0 && endDate >= fidx) {
			internalDate = payload.substring(fidx, endDate);
			asDate = parseDate(internalDate);
		} else {
			logger.error("Failed to get date in fetch response: " + payload);
			asDate = new Date();
		}

		int uid = parseUid(payload);

		Summary summary = new Summary(uid);

		InternalDate iDate = new InternalDate(uid, asDate);
		summary.setDate(iDate);

		FlagsList fl = parseFlags(payload);
		summary.setFlags(fl);

		summary.setSize(parseSize(payload));

		IMAPHeaders hs = HeadersParser.literalToHeaders(uid, r);
		summary.setHeaders(hs);
		return summary;
	}

	private FlagsList parseFlags(String payload) {
		FlagsList fl = new FlagsList();
		int fidx = payload.indexOf("FLAGS (") + 7; // "FLAGS (".length();
		int endFlags = payload.indexOf(')', fidx);
		String flags = "";
		if (fidx > 0 && endFlags >= fidx) {
			flags = payload.substring(fidx, endFlags);
		}
		FlagsStringParser.parse(flags, fl);

		return fl;
	}

	private int parseUid(String payload) {
		int uidIdx = payload.indexOf("UID ") + 4; // "UID ".length();
		int endUid = uidIdx;
		while (Character.isDigit(payload.charAt(endUid))) {
			endUid++;
		}
		int uid = Integer.parseInt(payload.substring(uidIdx, endUid));
		return uid;
	}

	private int parseSize(String payload) {
		int start = payload.indexOf("RFC822.SIZE ") + 12; // "RFC822.SIZE
															// ".length();
		int end = start;
		while (Character.isDigit(payload.charAt(end))) {
			end++;
		}
		int size = Integer.parseInt(payload.substring(start, end));
		return size;
	}

	private final Date parseDate(String date) {
		try {
			return DecoderUtils.decodeDateTime(date);
		} catch (Exception e) {
			logger.error("Can't parse '{}'", date, e);
			return new Date();
		}
	}

}
