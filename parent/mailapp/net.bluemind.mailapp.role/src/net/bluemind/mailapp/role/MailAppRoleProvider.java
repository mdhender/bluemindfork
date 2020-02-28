package net.bluemind.mailapp.role;

import java.util.Collections;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.bluemind.role.api.BasicRoles;
import net.bluemind.role.api.RoleDescriptor;
import net.bluemind.role.api.RolesCategory;
import net.bluemind.role.provider.IRolesProvider;

public class MailAppRoleProvider implements IRolesProvider {

	/**
	 * Permission to access Bluemind webmail.
	 */
	public static final String ROLE_MAIL_WEBAPP = "hasMailWebapp";

	@Override
	public Set<String> getRoles() {
		return ImmutableSet.<String>builder().add(ROLE_MAIL_WEBAPP).build();
	}

	@Override
	public Set<RoleDescriptor> getDescriptors(Locale locale) {
		ResourceBundle rb = ResourceBundle.getBundle("OSGI-INF/l10n/bundle", locale);

		RoleDescriptor accessBluemindWebmail = RoleDescriptor
				.create(ROLE_MAIL_WEBAPP, BasicRoles.CATEGORY_MAIL,
						rb.getString("role.accessMailWebapp.label"),
						rb.getString("role.accessMailWebapp.description"))
				.giveRoles(BasicRoles.ROLE_MAIL).delegable();

		return ImmutableSet.<RoleDescriptor>builder().add(accessBluemindWebmail).build();
	}

	@Override
	public Set<RolesCategory> getCategories(Locale locale) {
		return Collections.emptySet();
	}

}
