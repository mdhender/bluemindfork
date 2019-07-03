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
package net.bluemind.mailmessage.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.mailmessage.api.IMailTipEvaluation;

public class Activator implements BundleActivator {

	public static Map<String, List<IMailTipEvaluation>> mailtipHandlers = new HashMap<>();
	private static final Logger logger = LoggerFactory.getLogger(Activator.class);

	@Override
	public void start(BundleContext context) throws Exception {
		loadMailTipHandlers();
	}

	private void loadMailTipHandlers() {
		RunnableExtensionLoader<IMailTipEvaluation> epLoader = new RunnableExtensionLoader<>();
		List<IMailTipEvaluation> extensions = epLoader.loadExtensions("net.bluemind.mailmessage", "mailtip",
				"evaluation", "impl");
		logger.info("Found {} mailtip handlers", extensions.size());
		for (IMailTipEvaluation iMailTipEvaluation : extensions) {
			mailtipHandlers.computeIfAbsent(iMailTipEvaluation.mailtipType(), (k) -> new ArrayList<>())
					.add(iMailTipEvaluation);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {

	}

}
