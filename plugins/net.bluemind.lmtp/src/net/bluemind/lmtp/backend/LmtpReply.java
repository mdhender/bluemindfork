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
package net.bluemind.lmtp.backend;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.bluemind.lmtp.impl.LmtpConfig;
import net.bluemind.lmtp.impl.LmtpResponse;

public enum LmtpReply {

	/**
	 * 
	 */
	OK(250, "2.0.0", "OK"),

	/**
	 * 
	 */
	SENDER_OK(250, "2.0.0", "Sender OK"),

	/**
	 * 
	 */
	RECIPIENT_OK(250, "2.1.5", "Recipient OK"),

	/**
	 * 
	 */
	DELIVERY_OK(250, "2.1.5", "Delivery OK"),

	/**
	 * 
	 */
	OK_TO_SEND_DATA(354, null, "End data with <CR><LF>.<CR><LF>"),

	/**
	 * 
	 */
	USE_RCPT_INSTEAD(252, "2.3.3", "Use RCPT to deliver messages"),

	BYE(221, null, new DetailCB() {
		@Override
		protected String detail() {
			return LmtpConfig.getServerName() + " closing connection";
		}
	}),

	/**
	 * 
	 */
	GREETING(220, null, new DetailCB() {
		@Override
		protected String detail() {
			return LmtpConfig.getServerName() + " v" + LmtpConfig.getServerVersion() + " server ready";
		}
	}),

	SERVICE_DISABLED(421, "4.3.2", "Service not available, closing transmission channel"),

	/**
	 * 
	 */
	MAILBOX_DISABLED(450, "4.2.1", "Mailbox disabled, not accepting messages"),

	/**
	 * 
	 */
	MAILBOX_NOT_ON_THIS_SERVER(450, "4.2.0", "Mailbox is not on this server"),

	/**
	 * 
	 */
	TEMPORARY_FAILURE(451, "4.0.0", "Temporary message delivery failure try again"),

	/**
	 * 
	 */
	IO_ERROR(451, "4.3.0", "System I/O error"),

	/**
	 * 
	 */
	TEMPORARY_FAILURE_OVER_QUOTA(452, "4.2.2", "Over quota"),

	/**
	 * 
	 */
	TIMEOUT(421, null, new DetailCB() {
		@Override
		protected String detail() {
			return LmtpConfig.getServerName() + " Timeout exceeded";
		}
	}),

	/**
	 * 
	 */
	NESTED_MAIL_COMMAND(503, "5.5.1", "Nested MAIL command"),

	/**
	 * 
	 */
	NO_RECIPIENTS(503, "5.5.1", "No recipients"),

	/**
	 * 
	 */
	MISSING_MAIL_TO(503, "5.5.1", "Need MAIL command"),

	/**
	 * 
	 */
	SYNTAX_ERROR(500, "5.5.2", "Syntax error"),

	/**
	 * 
	 */
	INVALID_RECIPIENT_ADDRESS(500, "5.5.2", "Syntax error in recipient address"),

	/**
	 * 
	 */
	INVALID_SENDER_ADDRESS(501, "5.5.4", "Syntax error in sender address"),

	/**
	 * 
	 */
	INVALID_BODY_PARAMETER(501, "5.5.4", "Syntax error in BODY parameter"),

	/**
	 * 
	 */
	INVALID_SIZE_PARAMETER(501, "5.5.4", "Syntax error in SIZE parameter"),

	/**
	 * 
	 */
	NO_SUCH_USER(550, "5.1.1", "No such user here"),

	/**
	 * 
	 */
	PERMANENT_FAILURE_OVER_QUOTA(552, "5.2.2", "Over quota"),

	/**
	 * 
	 */
	PERMANENT_FAILURE(554, "5.0.0", "Permanent message delivery failure");

	private int mCode;
	private String mEnhancedCode;
	private String mDetail;
	private DetailCB mDetailCallback;

	private static final Map<String, LmtpReply> codesIndex = buildIndex();

	private static Map<String, LmtpReply> buildIndex() {
		Map<String, LmtpReply> idx = new HashMap<>();
		for (LmtpReply lr : LmtpReply.values()) {
			idx.put(lr.mCode + ":" + lr.mEnhancedCode, lr);
		}
		return ImmutableMap.copyOf(idx);
	}

	private abstract static class DetailCB {
		protected abstract String detail();
	}

	private LmtpReply(int code, String enhancedCode, String detail) {
		mCode = code;
		mEnhancedCode = enhancedCode;
		mDetail = detail;
	}

	private LmtpReply(int code, String enhancedCode, DetailCB detail) {
		mCode = code;
		mEnhancedCode = enhancedCode;
		mDetailCallback = detail;
	}

	public static LmtpReply adapt(LmtpResponse resp) {
		LmtpReply ret = null;
		if (resp.getResponseMessage().length() > 5) {
			String enhancedCode = resp.getResponseMessage().substring(0, 5);
			String key = resp.getCode() + ":" + enhancedCode;
			ret = codesIndex.get(key);
		}
		return ret;
	}

	@Override
	public String toString() {
		String detail;
		if (mDetailCallback != null)
			detail = mDetailCallback.detail();
		else
			detail = mDetail;
		if (mEnhancedCode == null)
			return mCode + " " + detail;
		else
			return mCode + " " + mEnhancedCode + " " + detail;
	}

	public boolean success() {
		if (mCode > 199 && mCode < 400)
			return true;
		return false;
	}

	public boolean isTemporaryFailure() {
		if (mCode > 399 && mCode < 500)
			return true;
		return false;
	}
}
