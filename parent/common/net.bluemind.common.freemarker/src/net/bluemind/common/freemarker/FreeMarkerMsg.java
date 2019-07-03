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
package net.bluemind.common.freemarker;

import java.util.List;
import java.util.ResourceBundle;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class FreeMarkerMsg implements TemplateMethodModelEx {

	private MessagesResolver messagesResolver;

	public FreeMarkerMsg(MessagesResolver messagesResolver) {
		this.messagesResolver = messagesResolver;

	}

	public FreeMarkerMsg(ResourceBundle... resources) {
		this(new MessagesResolver(resources));
	}

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
		Object[] o = new Object[arguments.size() - 1];
		for (int i = 1; i < arguments.size(); i++) {
			o[i - 1] = arguments.get(i);
		}
		return messagesResolver.translate(arguments.get(0).toString(), o);
	}

}
