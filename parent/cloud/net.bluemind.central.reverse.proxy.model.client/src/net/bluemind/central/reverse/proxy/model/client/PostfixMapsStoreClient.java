package net.bluemind.central.reverse.proxy.model.client;

import java.util.Collection;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import net.bluemind.central.reverse.proxy.model.client.impl.PostfixMapsStoreClientImpl;
import net.bluemind.central.reverse.proxy.model.common.DirInfo;
import net.bluemind.central.reverse.proxy.model.common.DomainInfo;
import net.bluemind.central.reverse.proxy.model.common.DomainSettings;
import net.bluemind.central.reverse.proxy.model.common.InstallationInfo;
import net.bluemind.central.reverse.proxy.model.common.MemberInfo;

public interface PostfixMapsStoreClient {

	static PostfixMapsStoreClient create(Vertx vertx) {
		return new PostfixMapsStoreClientImpl(vertx);
	}

	Future<Void> addInstallation(InstallationInfo installation);

	Future<Void> addDomain(DomainInfo info);

	Future<Void> addDomainSettings(DomainSettings domainSettings);

	Future<Void> addDir(DirInfo info);

	Future<Void> removeDir(String deletedUid);

	Future<Void> manageMember(MemberInfo member);

	Future<Collection<String>> aliasToMailboxes(String email);

	Future<Boolean> mailboxExists(String mailbox);

	Future<Boolean> mailboxDomainsManaged(String mailboxDomain);

	Future<String> getMailboxRelay(String mailbox);

	Future<String> srsRecipient(String recipient);
}
