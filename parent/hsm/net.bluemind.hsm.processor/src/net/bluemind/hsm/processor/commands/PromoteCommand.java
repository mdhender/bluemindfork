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
package net.bluemind.hsm.processor.commands;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.io.CountingInputStream;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.hsm.api.Promote;
import net.bluemind.hsm.api.TierChangeResult;
import net.bluemind.hsm.processor.HSMContext;
import net.bluemind.hsm.processor.HSMRunStats;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.IMAPRuntimeException;
import net.bluemind.imap.StoreClient;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.sysconf.helper.LocalSysconfCache;

public class PromoteCommand extends AbstractHSMCommand {
	private static final Logger logger = LoggerFactory.getLogger(PromoteCommand.class);

	private HSMContext context;
	private ArrayDeque<Promote> promote;

	public PromoteCommand(String folderPath, StoreClient storeClient, HSMContext context, ArrayDeque<Promote> promote) {
		super(folderPath, storeClient, context.getHSMStorage());
		this.context = context;
		this.promote = promote;
	}

	public List<TierChangeResult> run(HSMRunStats stats) throws IMAPException {
		List<TierChangeResult> ret = new ArrayList<>(promote.size());

		FlagsList fl = new FlagsList();
		fl.add(Flag.DELETED);

		while (!promote.isEmpty()) {
			Promote p = promote.poll();
			try {
				ret.add(promote(stats, p));

				sc.uidStore(Arrays.asList(p.imapUid), fl, true);
				sc.uidExpunge(Arrays.asList(p.imapUid));
			} catch (IOException ie) {
				logger.error("Fail to promote {}", p.hsmId, ie);
			} catch (IMAPRuntimeException ie) {
				throw new ServerFault("Fail to promote " + p.hsmId, ie);
			}
		}

		return ret;
	}

	private TierChangeResult promote(HSMRunStats stats, Promote p) throws IOException, IMAPException {
		p.flags.remove("bmarchived");

		if (sc.isClosed()) {
			sc = context.connect(folderPath);
		}

		FlagsList flags = FlagsList.fromString(Joiner.on(" ").join(p.flags));
		try (InputStream toRestore = storage
				.peek(context.getSecurityContext().getContainerUid(), context.getLoginContext().uid, p.hsmId,
						Optional.ofNullable(LocalSysconfCache.get().integerValue(SysConfKeys.message_size_limit.name()))
								.orElse(20 * 1024 * 1024));
				CountingInputStream cis = new CountingInputStream(toRestore)) {
			int restored = sc.append(folderPath, cis, flags, p.internalDate);
			if (sc.isClosed() || restored <= 0) {
				throw new IOException("Failed to append " + p.hsmId);
			}
			storage.delete(context.getSecurityContext().getContainerUid(), context.getLoginContext().uid, p.hsmId);

			stats.mailMoved();

			return TierChangeResult.create(restored, p.hsmId);
		}
	}
}
