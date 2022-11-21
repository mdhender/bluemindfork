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
package net.bluemind.core.api.fault;

import org.slf4j.helpers.MessageFormatter;

@SuppressWarnings("serial")
public class ServerFault extends RuntimeException {

	private ErrorCode code;

	public ServerFault() {
		this("Internal Error", ErrorCode.UNKNOWN);
	}

	public ServerFault(String s, Throwable t) {
		super(s, t);
		this.code = ErrorCode.UNKNOWN;
	}

	public ServerFault(String s, Throwable t, Object... args) {
		super(MessageFormatter.arrayFormat(s, args).getMessage(), t);
		this.code = ErrorCode.UNKNOWN;
	}

	public ServerFault(Throwable t) {
		super(t);
		this.code = ErrorCode.UNKNOWN;
	}

	public ServerFault(ServerFault t) {
		super(t);
		this.code = t.getCode();
	}

	public ServerFault(String s, Object... args) {
		this(MessageFormatter.arrayFormat(s, args).getMessage(), ErrorCode.UNKNOWN);
	}

	public ServerFault(String s, ErrorCode ec) {
		super(s);
		this.code = ec;
	}

	public ServerFault(String s, ErrorCode ec, Object... args) {
		this(s, args);
		this.code = ec;
	}

	public ErrorCode getCode() {
		return code;
	}

	public void setCode(ErrorCode code) {
		this.code = code;
	}

	public static ServerFault sqlFault(Exception t) {
		ServerFault sf = new ServerFault(t);
		sf.setCode(ErrorCode.SQL_ERROR);
		return sf;
	}

	public static ServerFault create(ErrorCode errorCode, Throwable t) {
		ServerFault sf = new ServerFault(t);
		sf.setCode(errorCode);
		return sf;
	}

	public static ServerFault notFound(String message, Object... args) {
		ServerFault sf = new ServerFault(message, args);
		sf.setCode(ErrorCode.NOT_FOUND);
		return sf;
	}

	public static ServerFault alreadyExists(String message, Object... args) {
		ServerFault sf = new ServerFault(message, args);
		sf.setCode(ErrorCode.ALREADY_EXISTS);
		return sf;
	}

	public static ServerFault tooManyResults(String message, Object... args) {
		ServerFault sf = new ServerFault(message, args);
		sf.setCode(ErrorCode.MAX_ITEM_COUNT);
		return sf;
	}
}
