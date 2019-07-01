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
package net.bluemind.filehosting.service;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.bluemind.role.api.RoleDescriptor;
import net.bluemind.role.api.RolesCategory;
import net.bluemind.role.provider.IRolesProvider;

public class FileHostingRolesProvider implements IRolesProvider {

	public static final String ROLE_ATTACHMENT = "canRemoteAttach";
	public static final String ROLE_DRIVE = "canUseFilehosting";
	public static final String CATEGORY_FILEHOSTING = "Filehosting";

	public FileHostingRolesProvider() {
	}

	@Override
	public Set<String> getRoles() {
		return ImmutableSet.<String>builder().add(ROLE_ATTACHMENT).add(ROLE_DRIVE).build();
	}

	@Override
	public Set<RoleDescriptor> getDescriptors(Locale locale) {
		ResourceBundle rb = ResourceBundle.getBundle("OSGI-INF/l10n/bundle", locale);

		RoleDescriptor attachment = RoleDescriptor.create(ROLE_ATTACHMENT, CATEGORY_FILEHOSTING,
				rb.getString("role.attachment.label"), rb.getString("role.attachment.description")).delegable();

		RoleDescriptor drive = RoleDescriptor.create(ROLE_DRIVE, CATEGORY_FILEHOSTING, rb.getString("role.drive.label"),
				rb.getString("role.drive.description")).delegable();

		return ImmutableSet.<RoleDescriptor>builder().add(attachment).add(drive).build();
	}

	@Override
	public Set<RolesCategory> getCategories(Locale locale) {
		ResourceBundle rb = ResourceBundle.getBundle("OSGI-INF/l10n/bundle", locale);

		return ImmutableSet.<RolesCategory>builder()
				.add(RolesCategory.create(CATEGORY_FILEHOSTING, rb.getString("category.filehosting"))).build();
	}

}
