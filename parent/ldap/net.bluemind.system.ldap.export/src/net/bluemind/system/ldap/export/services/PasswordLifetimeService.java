package net.bluemind.system.ldap.export.services;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.exception.LdapException;
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

public class PasswordLifetimeService {
	private static final Logger logger = LoggerFactory.getLogger(PasswordLifetimeService.class);

	public static Optional<PasswordLifetimeService> build(String domainUid) {
		if (domainUid == null || domainUid.isEmpty()) {
			throw new ServerFault("Invalid domain UID", ErrorCode.INVALID_PARAMETER);
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

		String passwordLifetime = context.provider().instance(IDomainSettings.class, domain.uid).get()
				.get(DomainSettingsKeys.password_lifetime.name());

		return Optional.of(new PasswordLifetimeService(ldapExportServers.get(0), domain, passwordLifetime));
	}

	private final ItemValue<Server> ldapExportServer;
	private final ItemValue<Domain> domain;
	private final String passwordLifetime;

	private PasswordLifetimeService(ItemValue<Server> ldapExportServer, ItemValue<Domain> domain,
			String passwordLifetime) {
		this.ldapExportServer = ldapExportServer;
		this.domain = domain;
		this.passwordLifetime = passwordLifetime;
	}

	public void sync() throws Exception {
		try (LdapConnection ldapCon = LdapHelper.connectDirectory(ldapExportServer)) {
			if (Strings.isNullOrEmpty(passwordLifetime)) {
				disablePasswordLifetime(ldapCon, passwordLifetime,
						getEntriesToUpdate(ldapCon, "(&(bmuid=*)(&(shadowMax=*)(!(shadowMax=0))))"));
				return;
			}

			Set<String> nerverExpireUserUids = getNeverExpireUserUid();

			// All users except those with must change password setting
			EntryCursor entriesToUpdate = getEntriesToUpdate(ldapCon, "(&(bmuid=*)(!(shadowMax=0)))");
			while (entriesToUpdate.next()) {
				logger.info(entriesToUpdate.get().getDn().getName());
				if (nerverExpireUserUids.contains(entriesToUpdate.get().get("bmUid").get().toString())) {
					continue;
				}

				ModifyResponse modifyResponse = ldapCon.modify(new ModifyRequestImpl()
						.setName(entriesToUpdate.get().getDn()).replace("shadowMax", passwordLifetime));
				if (modifyResponse.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS) {
					logger.error("Fail to update LDAP for domain {} with domain settings changes: {} - {}",
							domain.value.name, modifyResponse.getLdapResult().getResultCode(),
							modifyResponse.getLdapResult().getDiagnosticMessage());
				}
			}
		} catch (Exception e) {
			logger.error("Fail to update LDAP for domain {} with domain settings changes", domain.uid);
			throw e;
		}
	}

	private EntryCursor getEntriesToUpdate(LdapConnection ldapCon, String ldapFilter) throws LdapException {
		return ldapCon.search(new DomainDirectoryUsers(domain).getDn(), ldapFilter, SearchScope.SUBTREE, "bmUid");
	}

	private Set<String> getNeverExpireUserUid() throws SQLException {
		Set<String> neverExpireUserUids = new HashSet<>();

		String query = String.format("SELECT tci.uid AS uid FROM t_domain_user tdu " //
				+ "INNER JOIN t_container_item tci ON tci.id=tdu.item_id " //
				+ "INNER JOIN t_container tc ON tc.id=tci.container_id "//
				+ "WHERE password_neverexpires "//
				+ "AND tc.domain_uid='%s'", domain.uid);

		try (Connection conn = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext()
				.getDataSource().getConnection();
				PreparedStatement st = conn.prepareStatement(query);
				ResultSet rs = st.executeQuery()) {
			while (rs.next()) {
				neverExpireUserUids.add(rs.getString("uid"));
			}
		}

		return neverExpireUserUids;
	}

	private void disablePasswordLifetime(LdapConnection ldapCon, String passwordLifetime, EntryCursor entriesToUpdate)
			throws LdapException, CursorException {
		while (entriesToUpdate.next()) {
			ModifyResponse modifyResponse = ldapCon.modify(new ModifyRequestImpl()
					.setName(entriesToUpdate.get().getDn()).remove(new DefaultAttribute("shadowMax")));
			if (modifyResponse.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS) {
				logger.error("Fail to update LDAP for domain {} with domain settings changes: {} - {}",
						domain.value.name, modifyResponse.getLdapResult().getResultCode(),
						modifyResponse.getLdapResult().getDiagnosticMessage());
			}
		}
	}
}