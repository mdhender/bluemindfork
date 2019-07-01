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
package net.bluemind.system.ldap.importation.hooks;

import java.util.Collections;
import java.util.Map;

import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.ldap.importation.api.LdapProperties;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public class DomainSanitizer implements ISanitizer<Domain> {
	private final BmContext context;

	/**
	 * @param context
	 */
	public DomainSanitizer(BmContext context) {
		this.context = context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.core.sanitizer.ISanitizer#create(java.lang.Object)
	 */
	@Override
	public void create(Domain domain) {
		sanitizeDomainProperties(Collections.<String, String> emptyMap(), domain.properties);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.core.sanitizer.ISanitizer#sanitize(java.lang.Object)
	 */
	@Override
	public void update(Domain previous, Domain domain) {
		sanitizeDomainProperties(previous.properties, domain.properties);
	}

	private void sanitizeDomainProperties(Map<String, String> previousProperties, Map<String, String> properties) {
		if (!Boolean.valueOf(properties.get(LdapProperties.import_ldap_enabled.name()))) {
			return;
		}

		if (properties.get(LdapProperties.import_ldap_base_dn.name()) != null
				&& properties.get(LdapProperties.import_ldap_base_dn.name()).trim().isEmpty()) {
			properties.put(LdapProperties.import_ldap_base_dn.name(), null);
		}

		if (properties.get(LdapProperties.import_ldap_login_dn.name()) != null
				&& properties.get(LdapProperties.import_ldap_login_dn.name()).trim().isEmpty()) {
			properties.put(LdapProperties.import_ldap_login_dn.name(), null);
		}

		properties.put(LdapProperties.import_ldap_protocol.name(),
				previousOrDefaultIfNull(LdapProperties.import_ldap_protocol.getDefaultValue(),
						previousProperties.get(LdapProperties.import_ldap_protocol.name()),
						properties.get(LdapProperties.import_ldap_protocol.name())));

		properties.put(LdapProperties.import_ldap_accept_certificate.name(),
				previousOrDefaultIfNull(LdapProperties.import_ldap_accept_certificate.getDefaultValue(),
						previousProperties.get(LdapProperties.import_ldap_accept_certificate.name()),
						properties.get(LdapProperties.import_ldap_accept_certificate.name())));

		properties.put(LdapProperties.import_ldap_ext_id_attribute.name(),
				previousOrDefaultIfNull(LdapProperties.import_ldap_ext_id_attribute.getDefaultValue(),
						previousProperties.get(LdapProperties.import_ldap_ext_id_attribute.name()),
						properties.get(LdapProperties.import_ldap_ext_id_attribute.name())));

		properties.put(LdapProperties.import_ldap_user_filter.name(),
				previousOrDefaultIfNull(LdapProperties.import_ldap_user_filter.getDefaultValue(),
						previousProperties.get(LdapProperties.import_ldap_user_filter.name()),
						properties.get(LdapProperties.import_ldap_user_filter.name())));

		properties.put(LdapProperties.import_ldap_group_filter.name(),
				previousOrDefaultIfNull(LdapProperties.import_ldap_group_filter.getDefaultValue(),
						previousProperties.get(LdapProperties.import_ldap_group_filter.name()),
						properties.get(LdapProperties.import_ldap_group_filter.name())));

		if (!context.getSecurityContext().isDomainGlobal()) {
			properties.put(LdapProperties.import_ldap_lastupdate.name(),
					previousProperties.get(LdapProperties.import_ldap_lastupdate.name()));
		}
	}

	/**
	 * @param defaultValue
	 * @param previousValue
	 * @param value
	 */
	private String previousOrDefaultIfNull(String defaultValue, String previousValue, String value) {
		if (value != null && !value.trim().isEmpty()) {
			return value;
		}

		if (previousValue != null && !previousValue.trim().isEmpty()) {
			return previousValue;
		}

		return defaultValue;
	}
}
