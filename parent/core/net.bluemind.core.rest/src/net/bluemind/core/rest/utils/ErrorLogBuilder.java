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
package net.bluemind.core.rest.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.base.RestRequest;

public class ErrorLogBuilder {

	private static final List<String> stopClasses = Arrays.asList("ServiceMethodInvocation",
			"RestServiceMethodHandler");

	public static String build(Throwable exception) {
		StringBuilder sb = new StringBuilder();
		Throwable cause = exception.getCause();
		sb.append(toLog(exception));
		if (null != cause) {
			sb.append("Caused by:\r\n");
			sb.append(toLog(cause));
		}
		return sb.toString();
	}

	private static String toLog(Throwable cause) {
		StringBuilder sb = new StringBuilder(
				"  " + cause.getClass().toString() + ": " + cause.getMessage() + System.getProperty("line.separator"));
		if (cause instanceof ServerFault ex) {
			if (ex.getCode() == ErrorCode.PERMISSION_DENIED
					&& cause.getStackTrace()[0].toString().contains("RBACManager")) {
				return sb.toString();
			}
		}
		StackTraceElement[] stackTrace = cause.getStackTrace();
		List<String> buffer = new ArrayList<>();
		int handled = 0;
		for (StackTraceElement element : stackTrace) {
			if (isRestServiceCode(element)) {
				break;
			}
			buffer.add("\tat " + element.toString() + System.getProperty("line.separator"));
			if (isRelevant(element)) {
				buffer.forEach(s -> sb.append(s));
				handled += buffer.size();
				buffer = new ArrayList<>();
			}
		}
		if (handled < stackTrace.length) {
			int omitted = stackTrace.length - handled;
			sb.append("\t... " + omitted + " common frames omitted" + System.getProperty("line.separator"));
		}
		return sb.toString();
	}

	private static boolean isRelevant(StackTraceElement element) {
		return element.toString().contains("net.bluemind");
	}

	private static boolean isRestServiceCode(StackTraceElement element) {
		return stopClasses.stream().filter(s -> element.getClassName().contains(s)).count() > 0;
	}

	public static String filter(RestRequest request) {
		return String.format(
				"RestRequest [path=%s, method=%s, User-Agent=%s, params=%s, remoteAddresses=%s, origin=%s]",
				request.path, request.method, request.headers.get("User-Agent"), request.params,
				request.remoteAddresses, request.origin);
	}

}
