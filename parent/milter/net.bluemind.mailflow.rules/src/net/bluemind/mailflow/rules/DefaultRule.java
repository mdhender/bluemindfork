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
package net.bluemind.mailflow.rules;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.mailflow.rbe.MailRule;

public abstract class DefaultRule implements MailRule {

	protected Map<String, String> configuration = Collections.emptyMap();
	protected List<MailRule> children = Collections.emptyList();
	protected static final Logger logger = LoggerFactory.getLogger(DefaultRule.class);

	@Override
	public void receiveConfiguration(Map<String, String> configuration) {
		this.configuration = configuration;
	}

	@Override
	public void receiveChildren(List<MailRule> children) {
		this.children = children;
	}

}
