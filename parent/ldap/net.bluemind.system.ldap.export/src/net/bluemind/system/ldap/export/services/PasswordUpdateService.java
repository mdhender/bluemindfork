package net.bluemind.system.ldap.export.services;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.server.api.Server;
import net.bluemind.system.ldap.export.LdapHelper;
import net.bluemind.system.ldap.export.objects.DomainDirectoryUsers;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class PasswordUpdateService {
	private static final Logger logger = LoggerFactory.getLogger(PasswordUpdateService.class);

	public static Optional<PasswordUpdateService> build(String domainUid, String userUid) {
		if (domainUid == null || domainUid.isEmpty()) {
			throw new ServerFault("Invalid domain UID", ErrorCode.INVALID_PARAMETER);
		}

		if (userUid == null || userUid.isEmpty()) {
			throw new ServerFault("Invalid user UID", ErrorCode.INVALID_PARAMETER);
		}

		BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

		List<ItemValue<Server>> ldapExportServers = LdapExportService.ldapExportServer(context, domainUid);
		if (ldapExportServers.size() != 1) {
			return Optional.empty();
		}

		ItemValue<Domain> domain = context.provider().instance(IDomains.class, domainUid).get(domainUid);
		if (domain == null) {
			throw new ServerFault(String.format("Domain %s not found", domainUid), ErrorCode.UNKNOWN);
		}

		ItemValue<User> user = context.provider().instance(IUser.class, domainUid).getComplete(userUid);
		if (user == null) {
			throw new ServerFault(String.format("User %s not found", domainUid), ErrorCode.UNKNOWN);
		}

		String passwordLifetime = context.provider().instance(IDomainSettings.class, domain.uid).get()
				.get(DomainSettingsKeys.password_lifetime.name());

		return Optional.of(new PasswordUpdateService(ldapExportServers.get(0), domain, passwordLifetime, user));
	}

	private final ItemValue<Server> ldapExportServer;
	private final ItemValue<Domain> domain;
	private final String passwordLifetime;
	private final ItemValue<User> user;

	public PasswordUpdateService(ItemValue<Server> ldapExportServer, ItemValue<Domain> domain, String passwordLifetime,
			ItemValue<User> user) {
		this.ldapExportServer = ldapExportServer;
		this.domain = domain;
		this.passwordLifetime = passwordLifetime;
		this.user = user;
	}

	public void sync() throws Exception {
		try (LdapConnection ldapCon = LdapHelper.connectDirectory(ldapExportServer)) {
			EntryCursor result = ldapCon.search(new DomainDirectoryUsers(domain).getDn(),
					String.format("(bmuid=%s)", user.uid), SearchScope.SUBTREE);

			while (result.next()) {
				Entry entry = result.get();
				ModifyRequest mr = new ModifyRequestImpl().setName(entry.getDn()).replace("shadowLastChange",
						Long.toString(user.value.passwordLastChange.toInstant().atZone(ZoneId.systemDefault())
								.toLocalDate().toEpochDay()));

				if (!user.value.passwordNeverExpires && !Strings.isNullOrEmpty(passwordLifetime)) {
					mr.replace("shadowMax", passwordLifetime);
				} else if (entry.containsAttribute("shadowMax")) {
					mr.remove(new DefaultAttribute("shadowMax"));
				}

				ModifyResponse r = ldapCon.modify(mr);
				if (r.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS) {
					logger.error("Fail to update user {}@{}: {} - {}", user.value.login, domain.value.name,
							r.getLdapResult().getResultCode(), r.getLdapResult().getDiagnosticMessage());
				}
			}
		} catch (Exception e) {
			logger.error("Fail to update password last change for user {}@{}", user.value.login, domain.value.name);
			throw e;
		}
	}
}
