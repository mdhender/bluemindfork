/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.imap.driver.mailapi.search;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.RawImapBinding;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.imap.endpoint.cmd.ImapDateParser;
import net.bluemind.imap.endpoint.driver.SelectedFolder;

public class UidSearchFastPaths {

	public interface SearchFastPath {
		List<Long> search(SelectedFolder sel, String query);

		default boolean supports(String query) {
			return false;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(UidSearchFastPaths.class);
	private static final List<SearchFastPath> tunedSearches = List.of(new OutlookUidSince());

	public static Optional<SearchFastPath> lookup(String query) {
		return tunedSearches.stream().filter(sp -> sp.supports(query)).findAny().map(fp -> {
			logger.info("Using fast-path {} for '{}'", fp, query);
			return fp;
		});
	}

	private static class OutlookUidSince implements SearchFastPath {

		private static final Pattern outlookSearch = Pattern.compile("uid ([^\s]+) since (.+)",
				Pattern.CASE_INSENSITIVE);

		@Override
		public List<Long> search(SelectedFolder sel, String query) {
			var matcher = outlookSearch.matcher(query);
			matcher.find();
			String uidSet = matcher.group(1);
			Date date = ImapDateParser.readDate(matcher.group(2));

			List<RawImapBinding> raw = sel.recApi.imapIdSet(uidSet, "");
			List<Long> items = raw.stream().map(r -> r.itemId).toList();
			List<Long> output = new ArrayList<>(raw.size());
			for (var slice : Lists.partition(items, 256)) {
				List<ItemValue<MailboxRecord>> loaded = sel.recApi.multipleGetById(slice);
				for (var it : loaded) {
					if (date.before(it.value.internalDate)) {
						output.add(it.value.imapUid);
					}
				}
			}

			return output;
		}

		@Override
		public boolean supports(String query) {
			return outlookSearch.matcher(query).matches();
		}

		@Override
		public String toString() {
			return "OutlookUidSince";
		}

	}

}
