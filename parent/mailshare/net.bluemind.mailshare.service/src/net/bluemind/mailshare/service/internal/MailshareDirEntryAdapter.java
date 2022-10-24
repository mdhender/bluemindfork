package net.bluemind.mailshare.service.internal;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirValueStoreService.DirEntryAdapter;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailshare.api.Mailshare;

public class MailshareDirEntryAdapter implements DirEntryAdapter<Mailshare> {

	@Override
	public DirEntry asDirEntry(String domainUid, String uid, Mailshare mailshare) {
		return DirEntry.create(mailshare.orgUnitUid, domainUid + "/mailshares/" + uid, DirEntry.Kind.MAILSHARE, uid,
				getSummary(mailshare), mailshare.defaultEmailAddress(domainDefaultAlias(domainUid)), mailshare.hidden,
				mailshare.system, mailshare.archived, mailshare.dataLocation);
	}

	private String domainDefaultAlias(String domainName) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class)
				.findByNameOrAliases(domainName).value.defaultAlias;
	}

	private static String getSummary(Mailshare mailshare) {
		if (mailshare.card != null && mailshare.card.identification.formatedName.value != null) {
			return mailshare.card.identification.formatedName.value;
		} else {
			return mailshare.name;
		}
	}

}
