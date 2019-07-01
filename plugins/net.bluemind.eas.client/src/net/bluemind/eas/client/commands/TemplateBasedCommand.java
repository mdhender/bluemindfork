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
package net.bluemind.eas.client.commands;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ning.http.client.AsyncHttpClient;

import net.bluemind.eas.client.AccountInfos;
import net.bluemind.eas.client.IEasCommand;
import net.bluemind.eas.client.OPClient;
import net.bluemind.eas.utils.DOMUtils;

public abstract class TemplateBasedCommand<T> implements IEasCommand<T> {

	protected Document tpl;
	protected Boolean fromTemplate;
	protected Logger logger = LoggerFactory.getLogger(getClass());
	private String namespace;
	private String cmd;

	protected TemplateBasedCommand(NS namespace, String cmd, String templateName) {
		this.fromTemplate = true;
		this.namespace = namespace.toString();
		this.cmd = cmd;
		InputStream in = loadDataFile(templateName);
		if (in != null) {
			try {
				this.tpl = DOMUtils.parse(in);
			} catch (Exception e) {
				logger.error("error loading template " + templateName, e);
			}
		} else {
			logger.error("template " + templateName + " not found.");
		}
	}

	protected TemplateBasedCommand(NS namespace, String cmd, Document document) {
		this.fromTemplate = false;
		this.namespace = namespace.toString();
		this.cmd = cmd;
		this.tpl = document;
	}

	@Override
	public T run(AccountInfos ai, OPClient opc, AsyncHttpClient hc)
			throws Exception {
		if (fromTemplate) {
			customizeTemplate(ai, opc);
		}
		Document response = opc.postXml(namespace, tpl, cmd);
		T ret = parseResponse(response.getDocumentElement());
		return ret;
	}

	protected abstract void customizeTemplate(AccountInfos ai, OPClient opc);

	protected abstract T parseResponse(Element responseRootElem);

	private InputStream loadDataFile(String name) {
		return TemplateBasedCommand.class.getClassLoader().getResourceAsStream(
				"data/" + name);
	}

}
