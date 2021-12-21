/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.videoconferencing.hosting;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.bluemind.role.api.RoleDescriptor;
import net.bluemind.role.api.RolesCategory;
import net.bluemind.role.provider.IRolesProvider;

public class VideoConferencingRolesProvider implements IRolesProvider {

	public static final String ROLE_FULL_VISIO = "hasFullVideoconferencing";
	public static final String ROLE_VISIO = "hasSimpleVideoconferencing";
	public static final String CATEGORY_VIDEO_CONFERENCING = "Videoconferencing";

	@Override
	public Set<String> getRoles() {
		return ImmutableSet.<String>builder().add(ROLE_FULL_VISIO, ROLE_VISIO).build();
	}

	@Override
	public Set<RoleDescriptor> getDescriptors(Locale locale) {
		ResourceBundle rb = ResourceBundle.getBundle("OSGI-INF/l10n/bundle", locale);

		RoleDescriptor full = RoleDescriptor.create(ROLE_FULL_VISIO, CATEGORY_VIDEO_CONFERENCING,
				rb.getString("role.full.visio.label"), rb.getString("role.full.visio.description")).delegable()
				.notVisible();

		RoleDescriptor visio = RoleDescriptor.create(ROLE_VISIO, CATEGORY_VIDEO_CONFERENCING,
				rb.getString("role.simple.visio.label"), rb.getString("role.simple.visio.description")).delegable();

		return ImmutableSet.<RoleDescriptor>builder().add(full).add(visio).build();
	}

	@Override
	public Set<RolesCategory> getCategories(Locale locale) {
		ResourceBundle rb = ResourceBundle.getBundle("OSGI-INF/l10n/bundle", locale);

		return ImmutableSet.<RolesCategory>builder()
				.add(RolesCategory.create(CATEGORY_VIDEO_CONFERENCING, rb.getString("category.visio"))).build();
	}

}
