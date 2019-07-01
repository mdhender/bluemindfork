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
package net.bluemind.calendar.pdf.internal;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class MessageResolverMethod implements TemplateMethodModelEx {

	private ResourceBundle bundle;
	private Locale locale;

	public MessageResolverMethod(ResourceBundle bundle, Locale locale) {
		this.bundle = bundle;
		this.locale = locale;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments) throws TemplateModelException {
		if (arguments.size() < 1) {
			throw new TemplateModelException("Wrong number of arguments");
		}
		String code = arguments.get(0).toString();
		if (code == null || code.isEmpty()) {
			throw new TemplateModelException("Invalid code value '" + code + "'");
		}

		String message = bundle.getString(code);
		MessageFormat mf = new MessageFormat("");
		mf.setLocale(locale);
		mf.applyPattern(message);
		return mf.format(arguments.toArray());
	}

}
