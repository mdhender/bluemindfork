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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.io.CountingInputStream;

import net.bluemind.hsm.api.Promote;
import net.bluemind.hsm.api.TierChangeResult;
import net.bluemind.hsm.processor.HSMContext;
import net.bluemind.hsm.processor.HSMRunStats;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;

public class PromoteCommand extends AbstractHSMCommand {

	private static final Logger logger = LoggerFactory.getLogger(PromoteCommand.class);

	private HSMContext context;
	private Collection<Promote> promote;

	public PromoteCommand(String folderPath, StoreClient storeClient, HSMContext context, Collection<Promote> promote) {
		super(folderPath, storeClient, context.getHSMStorage());
		this.context = context;
		this.promote = promote;
	}

	public List<TierChangeResult> run(HSMRunStats stats) {

		List<TierChangeResult> ret = new ArrayList<TierChangeResult>(promote.size());

		Iterator<Promote> it = promote.iterator();
		while (it.hasNext()) {
			Promote p = it.next();
			try {
				ret.add(promote(stats, p));
			} catch (IOException io) {
				logger.error("Fail to promote {}", p.hsmId, io);
			}
		}

		List<Integer> mailUids = promote.stream().map(p -> p.imapUid).collect(Collectors.toList());

		if (!mailUids.isEmpty()) {
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			sc.uidStore(mailUids, fl, true);
			sc.uidExpunge(mailUids);
		}
		return ret;

	}

	private TierChangeResult promote(HSMRunStats stats, Promote p) throws IOException {
		p.flags.remove("bmarchived");

		FlagsList flags = FlagsList.fromString("(" + Joiner.on(" ").join(p.flags) + ")");
		try (InputStream toRestore = storage.take(context.getSecurityContext().getContainerUid(),
				context.getLoginContext().uid, p.hsmId); CountingInputStream cis = new CountingInputStream(toRestore)) {
			int restored = sc.append(folderPath, cis, flags, p.internalDate);
			if (restored <= 0) {
				throw new IOException("Failed to append " + p.hsmId);
			}

			stats.mailMoved();

			return TierChangeResult.create(restored, p.hsmId);
		}

	}

}
