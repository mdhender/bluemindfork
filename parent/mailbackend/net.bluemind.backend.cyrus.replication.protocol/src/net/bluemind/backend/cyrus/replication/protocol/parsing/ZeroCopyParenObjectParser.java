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

import java.nio.CharBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonElement;
import org.vertx.java.core.json.JsonObject;

import com.google.common.base.CharMatcher;

public final class ZeroCopyParenObjectParser implements ParenObjectParser {

	private static final CharBuffer EMPTY_BUFFER = CharBuffer.wrap("");

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ZeroCopyParenObjectParser.class);

	private JsonElement root;

	private static final class ParsingResult {
		private final JsonElement el;
		private final CharBuffer toConsume;

		private ParsingResult(JsonElement el, CharBuffer toConsume) {
			this.el = el;
			this.toConsume = toConsume;
		}

		public static ParsingResult of(JsonElement el, CharBuffer s) {
			return new ParsingResult(el, s);
		}
	}

	public ZeroCopyParenObjectParser() {
	}

	@Override
	public JsonElement parse(String s) {
		ParsingResult parsed = parse(ParsingResult.of(null, CharBuffer.wrap(s)));
		return parsed.el;
	}

	private CharBuffer trimLeadingWhitespace(CharBuffer sequence) {
		int len = sequence.length();
		for (int first = 0; first < len; first++) {
			if (!CharMatcher.whitespace().matches(sequence.charAt(first))) {
				return sequence.subSequence(first, len);
			}
		}
		return EMPTY_BUFFER;
	}

	private boolean isNullOrEmpty(CharSequence s) {
		return s == null || s.length() == 0;
	}

	private int indexOf(CharSequence sequence, char c, int start) {
		int len = sequence.length();
		for (int first = start; first < len; first++) {
			if (sequence.charAt(first) == c) {
				return first;
			}
		}
		return -1;
	}

	private int indexOf(CharSequence sequence, char c) {
		return indexOf(sequence, c, 0);
	}

	private ParsingResult parse(ParsingResult result) {
		JsonElement parent = result.el;
		CharBuffer s = trimLeadingWhitespace(result.toConsume);
		if (isNullOrEmpty(s)) {
			return ParsingResult.of(parent == null ? root : parent, EMPTY_BUFFER);
		}
		JsonElement ret = null;
		char firstChar = s.charAt(0);
		CharBuffer sub = null;
		CharBuffer remainder = null;
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
			remainder = s.subSequence(afterObject, s.length());
			break;
		case '(':
			// logger.info("parseArray");
			ret = new JsonArray();
			sub = matchParen(0, s);
			fillArray(ret.asArray(), trimLeadingWhitespace(sub));
			int afterArray = "()".length() + sub.length();
			remainder = s.subSequence(afterArray, s.length());
			break;
		default:
			// logger.info("keyVal, parent is array: " + parent.isArray());
			ret = parent;
			remainder = keyAndValue(parent, s);
		}
		if (root == null && ret != null) {
			root = ret;
		}
		if (remainder.length() > 0 && remainder.charAt(0) == ' ') {
			remainder = remainder.subSequence(1, remainder.length());
		}
		parse(ParsingResult.of(parent, remainder));
		return ParsingResult.of(ret, EMPTY_BUFFER);
	}

	private void fillArray(JsonArray array, CharBuffer sub) {
		if (sub.length() == 0) {
			return;
		}
		char arrayItem = sub.charAt(0);
		if (arrayItem == '%') {
			int remainderIdx = 1;
			int remainderLen = sub.length();
			while (remainderIdx < remainderLen) {
				CharBuffer inParens = matchParen(remainderIdx, sub);
				CharBuffer copy = CharBuffer.wrap("%(" + inParens.toString() + ")");
				int elemLen = copy.length();
				parse(ParsingResult.of(array, copy));
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
					next = indexOf(sub, '"', curIdx + 1);
					curIdx++;
					bumpCur = true;
				} else {
					next = indexOf(sub, ' ', curIdx + 1);
				}
				if (next == -1) {
					next = total;
				}
				CharBuffer tok = sub.subSequence(curIdx, next);
				array.addString(atomOrValue(tok.toString()));
				curIdx = next + 1;
				if (bumpCur) {
					curIdx++;
				}
			}
		}
	}

	private void fillObject(JsonObject obj, CharBuffer props) {
		parse(ParsingResult.of(obj, props));
	}

	private CharBuffer keyAndValue(JsonElement parent, CharBuffer s) {
		int space = indexOf(s, ' ');
		if (space < 0) {
			return EMPTY_BUFFER;
		}
		CharBuffer key = s.subSequence(0, space);
		// logger.debug("On key {}", key);
		CharBuffer remaining = s.subSequence(space + 1, s.length());
		char valueStart = remaining.charAt(0);
		int end = 0;
		CharBuffer value = null;
		switch (valueStart) {
		case '"':
			// simple qstring
			end = indexOf(remaining, '"', 1);
			value = remaining.subSequence(1, end);
			putValue(parent, key.toString(), value.toString());
			end = end + 1;
			break;
		case '%':
			ParsingResult parsedObject = parse(ParsingResult.of(parent, remaining));
			// the call to parse will deal with the stuff after the object
			end = remaining.length();
			parent.asObject().putObject(key.toString(), parsedObject.el.asObject());
			break;
		case '(':
			// logger.debug("inArrayValue: '{}'", remaining);
			ParsingResult parsedArray = parse(ParsingResult.of(parent, remaining));
			// the call to parse will deal with the stuff after the array
			end = remaining.length();
			parent.asObject().putArray(key.toString(), parsedArray.el.asArray());
			break;
		default:
			end = indexOf(remaining, ' ', 1);
			if (end > 0) {
				value = remaining.subSequence(0, end);
			} else {
				value = remaining;
				end = remaining.length();
			}
			putValue(parent, key.toString(), value.toString());
			break;
		}
		int max = remaining.length();
		int toGrab = end + 1;
		if (toGrab < max) {
			CharBuffer ret = remaining.subSequence(toGrab, max);
			// logger.debug("AfterKeyAndValue: '{}'", ret);
			return ret;
		} else {
			// logger.debug("AfterKeyAndValue, FINISHED.");
			return EMPTY_BUFFER;

		}
	}

	private void putValue(JsonElement parent, String key, String value) {
		String v = atomOrValue(value);
		parent.asObject().putString(key, v);
	}

	private String atomOrValue(String value) {
		return value;
	}

	private CharBuffer matchParen(int openParenIdx, CharBuffer s) {
		final int max = s.length();
		int depth = 1;
		int endIndex = openParenIdx;
		boolean inQuote = false;
		for (int i = openParenIdx + 1; i < max; i++) {
			char c = s.charAt(i);
			if (c == '"') {
				inQuote = !inQuote;
			}
			if (!inQuote) {
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
		}
		return s.subSequence(openParenIdx + 1, endIndex);
	}

}
