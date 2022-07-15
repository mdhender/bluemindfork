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
package net.bluemind.imap.endpoint.exec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.bluemind.imap.endpoint.cmd.AnalyzedCommand;

public class Processors {

	private Processors() {
	}

	private static final Map<Class<? extends AnalyzedCommand>, CommandProcessor<? extends AnalyzedCommand>> PROCS = new ConcurrentHashMap<>();

	static {
		register(new LoginProcessor());
		register(new NoopProcessor());
		register(new CapabilityProcessor());
		register(new IdProcessor());
		register(new SelectProcessor());
		register(new ExamineProcessor());
		register(new StatusProcessor());
		register(new MyRightsProcessor());
		register(new GetQuotaRootProcessor());
		register(new XListProcessor());
		register(new ListProcessor());
		register(new LsubProcessor());
		register(new UidFetchProcessor());
		register(new IdleProcessor());
		register(new DoneProcessor());
		register(new AppendProcessor());
		register(new LogoutProcessor());
	}

	private static final void register(CommandProcessor<?> proc) {
		PROCS.put(proc.handledType(), proc);
	}

	@SuppressWarnings("unchecked")
	public static <T extends AnalyzedCommand> CommandProcessor<T> get(Class<T> class1) {
		return (CommandProcessor<T>) PROCS.get(class1);
	}

}
