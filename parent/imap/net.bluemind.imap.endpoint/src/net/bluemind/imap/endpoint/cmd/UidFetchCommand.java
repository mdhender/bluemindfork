/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.endpoint.cmd;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.imap.endpoint.EndpointRuntimeException;
import net.bluemind.imap.endpoint.driver.MailPart;

public class UidFetchCommand extends AnalyzedCommand {

	private static final Pattern fetchTemplate = Pattern.compile("uid fetch ([^\\s]+) (.*)$", Pattern.CASE_INSENSITIVE);
	private List<MailPart> fields;
	private String idset;

	public UidFetchCommand(RawImapCommand raw) {
		super(raw);
		String fetch = flattenAtoms(true).fullCmd;
		Matcher m = fetchTemplate.matcher(fetch);

		if (m.find()) {
			idset = m.group(1);
			String props = m.group(2);
			this.fields = fetchSpec(props);
		} else {
			throw new EndpointRuntimeException("Cannot analyze fetch cmd " + fetch);
		}
	}

	public List<MailPart> fetchSpec() {
		return fields;
	}

	public String idset() {
		return idset;
	}

	/**
	 * Deals with
	 * <code>(UID RFC822.SIZE FLAGS BODY.PEEK[HEADER.FIELDS (From To Cc Bcc Subject Date Message-ID Priority X-Priority References Newsgroups In-Reply-To Content-Type Reply-To)])</code>
	 * style of input.
	 * 
	 * @param base
	 * @return
	 */
	private List<MailPart> fetchSpec(String base) {
		JsonArray lastPop = null;
		Stack<JsonArray> stack = new Stack<>();// NOSONAR
		JsonObject curObj = null;
		for (int i = 0; i < base.length(); i++) {
			char cur = base.charAt(i);
			switch (cur) {
			case '(':
			case '[':
			case '<':
				JsonArray child = new JsonArray();
				stack.add(child);
				if (curObj != null) {
					curObj.put("child", child);
				}
				curObj = new JsonObject();
				curObj.put("id", "");
				stack.peek().add(curObj);
				break;
			case ')':
			case ']':
			case '>':
				lastPop = stack.pop();
				break;
			case ' ':
				curObj = new JsonObject();
				curObj.put("id", "");
				stack.peek().add(curObj);
				break;
			default:
				String newVal = curObj.getString("id") + cur;
				curObj.put("id", newVal);
				break;
			}
		}
		if (lastPop == null) {
			throw new EndpointRuntimeException("imbalanced fetch spec: " + base);
		}

		int len = lastPop.size();
		List<MailPart> ret = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			MailPart mp = new MailPart();
			ret.add(mp);
			JsonObject js = lastPop.getJsonObject(i);
			mp.name = js.getString("id").toUpperCase();
			if (js.containsKey("child")) {
				JsonArray child = js.getJsonArray("child");
				JsonObject sectJs = child.getJsonObject(0);
				mp.section = sectJs.getString("id").toUpperCase();
				if (child.size() == 2) {
					mp.options = new LinkedHashSet<>();
					JsonArray sectOptsJs = child.getJsonObject(1).getJsonArray("child");
					for (int j = 0; j < sectOptsJs.size(); j++) {
						mp.options.add(sectOptsJs.getJsonObject(j).getString("id"));
					}
				} else if (sectJs.containsKey("child") && sectJs.getJsonArray("child").size() == 1) {
					mp.partial = sectJs.getJsonArray("child").getJsonObject(0).getString("id");
				}
			}
		}
		return ret;
	}

}
