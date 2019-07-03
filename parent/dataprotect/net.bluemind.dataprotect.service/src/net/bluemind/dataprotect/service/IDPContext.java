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

package net.bluemind.dataprotect.service;

import java.util.List;
import java.util.Set;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.server.api.Server;

public interface IDPContext {

	public interface IToolConfig {

		ItemValue<Server> getSource();

		String getTag();

		Set<String> getDirs();

	}

	public interface IToolSession {

		PartGeneration backup(PartGeneration previous, PartGeneration next);

		void restore(int partId, Set<String> what, String to);

		void restore(int partId, Set<String> what) throws ServerFault;

		void restoreOneFolder(int partId, String what, String to);

		String tmpDirectory();

		public void clean(List<Integer> validPartIds);

	}

	public interface ITool {
		IToolConfig configure(ItemValue<Server> source, String tag, Set<String> dirs);

		IToolSession newSession(IToolConfig tc);
	}

	ITool tool();

	void info(String locale, String msg);

	void warn(String locale, String msg);

	void error(String locale, String msg);
}
