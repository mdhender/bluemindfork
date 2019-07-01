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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.cyrus.replication.protocol.parsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonElement;
import org.vertx.java.core.json.JsonObject;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

public final class StringBasedParenObjectParser implements ParenObjectParser {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(StringBasedParenObjectParser.class);

	private JsonElement root;

	private static class ParsingResult {
		private final JsonElement el;
		private final String toConsume;

		private ParsingResult(JsonElement el, String toConsume) {
			this.el = el;
			this.toConsume = toConsume;
		}

		public static ParsingResult of(JsonElement el, String s) {
			return new ParsingResult(el, s);
		}
	}

	public StringBasedParenObjectParser() {
	}

	@Override
	public JsonElement parse(String s) {
		ParsingResult parsed = parse(ParsingResult.of(null, s));
		return parsed.el;
	}

	private ParsingResult parse(ParsingResult result) {
		JsonElement parent = result.el;
		String s = CharMatcher.whitespace().trimLeadingFrom(result.toConsume);
		if (Strings.isNullOrEmpty(s)) {
			return ParsingResult.of(parent == null ? root : parent, "");
		}
		JsonElement ret = null;
		char firstChar = s.charAt(0);
		String sub = null;
		String remainder = null;
		switch (firstChar) {
		case '%':
			// logger.info("parseObject");
			ret = new JsonObject();
			sub = matchParen(1, s);
			fillObject(ret.asObject(), sub);
			if (parent != null && parent.isArray()) {
				parent.asArray().addObject(ret.asObject());
			}
			int afterObject = "%()".length() + sub.length();
			remainder = s.substring(afterObject);
			break;
		case '(':
			// logger.info("parseArray");
			ret = new JsonArray();
			sub = matchParen(0, s);
			fillArray(ret.asArray(), CharMatcher.whitespace().trimLeadingFrom(sub));
			int afterArray = "()".length() + sub.length();
			remainder = s.substring(afterArray);
			break;
		default:
			// logger.info("keyVal, parent is array: " + parent.isArray());
			ret = parent;
			remainder = keyAndValue(parent, s);
		}
		if (root == null && ret != null) {
			root = ret;
		}
		if (remainder.startsWith(" ")) {
			remainder = remainder.substring(1);
		}
		parse(ParsingResult.of(parent, remainder));
		return ParsingResult.of(ret, "");
	}

	private void fillArray(JsonArray array, String sub) {
		if (sub.isEmpty()) {
			return;
		}
		char arrayItem = sub.charAt(0);
		if (arrayItem == '%') {
			int remainderIdx = 1;
			int remainderLen = sub.length();
			while (remainderIdx < remainderLen) {
				String arrayElem = "%(" + matchParen(remainderIdx, sub) + ")";
				int elemLen = arrayElem.length();
				parse(ParsingResult.of(array, arrayElem));
				// jump to the opening paren that starts the next object in the array
				remainderIdx += elemLen;
				while (remainderIdx < remainderLen && sub.charAt(remainderIdx) != '(') {
					remainderIdx++;
				}
			}
		} else {
			int curIdx = 0;
			int total = sub.length();
			while (curIdx < total) {
				int next = curIdx;
				boolean bumpCur = false;
				if (sub.charAt(curIdx) == '"') {
					next = sub.indexOf('"', curIdx + 1);
					curIdx++;
					bumpCur = true;
				} else {
					next = sub.indexOf(' ', curIdx + 1);
				}
				if (next == -1) {
					next = total;
				}
				String tok = sub.substring(curIdx, next);
				array.addString(atomOrValue(tok));
				curIdx = next + 1;
				if (bumpCur) {
					curIdx++;
				}
			}
		}
	}

	private void fillObject(JsonObject obj, String props) {
		parse(ParsingResult.of(obj, props));
	}

	private String keyAndValue(JsonElement parent, String s) {
		int space = s.indexOf(' ');
		if (space < 0) {
			return "";
		}
		String key = s.substring(0, space);
		// logger.debug("On key {}", key);
		String remaining = s.substring(space + 1);
		char valueStart = remaining.charAt(0);
		int end = 0;
		String value = null;
		switch (valueStart) {
		case '"':
			// simple qstring
			end = remaining.indexOf('"', 1);
			value = remaining.substring(1, end);
			putValue(parent, key, value);
			end = end + 1;
			break;
		case '%':
			ParsingResult parsedObject = parse(ParsingResult.of(parent, remaining));
			// the call to parse will deal with the stuff after the object
			end = remaining.length();
			parent.asObject().putObject(key, parsedObject.el.asObject());
			break;
		case '(':
			// logger.debug("inArrayValue: '{}'", remaining);
			ParsingResult parsedArray = parse(ParsingResult.of(parent, remaining));
			// the call to parse will deal with the stuff after the array
			end = remaining.length();
			parent.asObject().putArray(key, parsedArray.el.asArray());
			break;
		default:
			end = remaining.indexOf(' ', 1);
			if (end > 0) {
				value = remaining.substring(0, end);
			} else {
				value = remaining;
				end = remaining.length();
			}
			putValue(parent, key, value);
			break;
		}
		int max = remaining.length();
		int toGrab = end + 1;
		if (toGrab < max) {
			String ret = remaining.substring(end + 1);
			// logger.debug("AfterKeyAndValue: '{}'", ret);
			return ret;
		} else {
			// logger.debug("AfterKeyAndValue, FINISHED.");
			return "";

		}
	}

	private void putValue(JsonElement parent, String key, String value) {
		String v = atomOrValue(value);
		parent.asObject().putString(key, v);
	}

	private String atomOrValue(String value) {
		return value;
	}

	private String matchParen(int openParenIdx, String s) {
		final int max = s.length();
		int depth = 1;
		int endIndex = openParenIdx;
		for (int i = openParenIdx + 1; i < max; i++) {
			char c = s.charAt(i);
			if (c == '(') {
				depth++;
			} else if (c == ')') {
				depth--;
				if (depth == 0) {
					endIndex = i;
					break;
				}
			}
		}
		return s.substring(openParenIdx + 1, endIndex);
	}

}
