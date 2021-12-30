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
package net.bluemind.milter.impl.map;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.milter.map.RecipientCanonical;
import net.bluemind.milter.map.RecipientCanonicalFactory;

public class RecipientCanonicalRegistry {
	private static Logger logger = LoggerFactory.getLogger(RecipientCanonicalRegistry.class);

	private static List<RecipientCanonical> loaded;

	static {
		init();
	}

	public final static Collection<RecipientCanonical> get() {
		return loaded;
	}

	private static final void init() {
		logger.info("loading net.bluemind.milter.recipientcanonicalfactory extensions");
		RunnableExtensionLoader<RecipientCanonicalFactory> rel = new RunnableExtensionLoader<RecipientCanonicalFactory>();
		loaded = rel
				.loadExtensions("net.bluemind.milter", "recipientcanonicalfactory", "recipientcanonical_factory",
						"impl")
				.stream().map(RecipientCanonicalFactory::create).filter(Objects::nonNull).collect(Collectors.toList());

		logger.info("{} implementation found for extensionpoint net.bluemind.milter.recipientcanonicalfactory",
				loaded.size());
	}

}
