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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.index.mail.ring.actions;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.IDomains;
import net.bluemind.index.mail.MailIndexService;
import net.bluemind.index.mail.ring.AliasRing.RingAlias;
import net.bluemind.index.mail.ring.AliasRing.RingIndex;
import net.bluemind.lib.elasticsearch.IndexAliasMapping.RingIndexAliasMapping;

public class CopyDocumentsAction implements IndexAction {

	private final RingIndex sourceIndex;
	private final SortedSet<RingAlias> concernedAliases;
	private final String targetIndex;
	private final MailIndexService service;

	public CopyDocumentsAction(MailIndexService service, RingIndex sourceIndex, SortedSet<RingAlias> concernedAliases,
			String targetIndex) {
		this.service = service;
		this.sourceIndex = sourceIndex;
		this.concernedAliases = concernedAliases;
		this.targetIndex = targetIndex;
	}

	@Override
	public void execute(ElasticsearchClient esClient) throws ElasticsearchException, IOException {
		var concernedMailboxes = getConcernedMailboxes(concernedAliases);
		concernedMailboxes.forEach(box -> {
			service.moveMailspoolBox(esClient, box, sourceIndex.name(), targetIndex);
			service.bulkDelete(sourceIndex.name(), q -> q.term(t -> t.field("owner").value(box)));
		});
	}

	private Set<String> getConcernedMailboxes(SortedSet<RingAlias> aliases) {
		var aliasNames = aliases.stream().map(RingAlias::name).toList();
		var provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		var concerndedMailboxes = new HashSet<String>();
		var domains = provider.instance(IDomains.class);
		domains.all().forEach(domain -> {
			var directory = provider.instance(IDirectory.class, domain.uid);

			var query = DirEntryQuery.filterKind(BaseDirEntry.Kind.GROUP, BaseDirEntry.Kind.USER,
					BaseDirEntry.Kind.MAILSHARE, BaseDirEntry.Kind.RESOURCE);
			concerndedMailboxes.addAll(directory.search(query).values.stream() //
					.map(entry -> entry.uid) //
					.filter(entry -> aliasNames.contains(new RingIndexAliasMapping().getReadAliasByMailboxUid(entry)))
					.toList());
		});

		return concerndedMailboxes;
	}

	@Override
	public String info() {
		return "Copying documents of mailboxes "
				+ String.join(",", concernedAliases.stream().map(RingAlias::name).toList()) + " from index "
				+ sourceIndex.name() + " to " + targetIndex;
	}

}
