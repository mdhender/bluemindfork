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
package net.bluemind.mailflow.service;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.mailflow.api.MailActionDescriptor;
import net.bluemind.mailflow.api.MailRuleDescriptor;

public class MailFlowRegistry {

	private static final Logger logger = LoggerFactory.getLogger(MailFlowRegistry.class);

	private static List<MailRuleDescriptor> mailFlowRules;
	private static List<MailActionDescriptor> mailFlowActions;

	static {
		loadRuleIdentifiers();
		loadActionIdentifiers();
	}

	private static void loadRuleIdentifiers() {
		mailFlowRules = new ArrayList<>();
		logger.info("loading net.bluemind.mailflow.rules extensions");
		String pluginId = "net.bluemind.mailflow";
		String pointName = "rules_extensions";

		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(pluginId, pointName);
		IExtension[] extensions = point.getExtensions();
		for (IExtension ie : extensions) {
			MailRuleDescriptor descriptor = new MailRuleDescriptor();
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if ("rule".equals(e.getName())) {
					descriptor.description = e.getAttribute("description");
					descriptor.ruleIdentifier = e.getAttribute("ruleIdentifier");
				}
			}
			mailFlowRules.add(descriptor);
		}
	}

	private static void loadActionIdentifiers() {
		mailFlowActions = new ArrayList<>();
		logger.info("loading net.bluemind.mailflow.actions extensions");
		String pluginId = "net.bluemind.mailflow";
		String pointName = "actions_extensions";
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(pluginId, pointName);
		IExtension[] extensions = point.getExtensions();
		for (IExtension ie : extensions) {
			MailActionDescriptor descriptor = new MailActionDescriptor();
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if ("action".equals(e.getName())) {
					descriptor.description = e.getAttribute("description");
					descriptor.actionIdentifier = e.getAttribute("actionIdentifier");
					descriptor.actionContext = e.getAttribute("actionContext");
				}
			}
			mailFlowActions.add(descriptor);
		}
	}

	public static List<MailActionDescriptor> getActions() {
		return mailFlowActions;
	}

	public static List<MailRuleDescriptor> getRules() {
		return mailFlowRules;
	}

}
