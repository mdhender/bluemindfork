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
package net.bluemind.calendar.pdf.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.bluemind.calendar.api.PrintOptions;
import net.bluemind.calendar.api.PrintOptions.CalendarMetadata;
import net.bluemind.calendar.pdf.internal.PrintCalendar.CalInfo;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.user.api.IUserSettings;

public class PrintContext {

	public final Map<String, String> userSettings;
	public final List<CalInfo> calendars;
	public final SecurityContext securityContext;

	public PrintContext(Map<String, String> userSettings, List<CalInfo> calendars, SecurityContext securityContext) {
		this.userSettings = userSettings;
		this.calendars = calendars;
		this.securityContext = securityContext;
	}

	public static PrintContext create(BmContext context, PrintOptions options) throws ServerFault {
		Map<String, String> settings = context.provider()
				.instance(IUserSettings.class, context.getSecurityContext().getContainerUid())
				.get(context.getSecurityContext().getSubject());

		List<CalInfo> calendars = new ArrayList<>(options.calendars.size());
		IContainers containers = context.provider().instance(IContainers.class);

		int pos = 0;
		for (CalendarMetadata cm : options.calendars) {
			ContainerDescriptor cal = containers.get(cm.uid);
			CalInfo calInfo = buildCalInfo(cal, cm, pos++, options.color);
			calendars.add(calInfo);
		}

		return new PrintContext(settings, calendars, context.getSecurityContext());

	}

	private static CalInfo buildCalInfo(ContainerDescriptor cal, CalendarMetadata cm, int pos, boolean colored) {
		String color = null;
		if (!colored) {
			color = "#D3D3D3";
		} else {
			color = cm.color;
			if (color == null) {
				color = cal.settings.get("bm_color");
				if (color == null) {
					color = ColorPalette.DEFAULT_COLORS[pos];
				}
			}
		}

		CalInfo ret = new CalInfo();
		ret.uid = cal.uid;
		ret.name = cal.name;
		ret.color = color;
		ret.colorLighter = ColorPalette.lighter(color);
		ret.colorDarker = ColorPalette.darker(color);
		ret.colorDarkerDarker = ColorPalette.darker(ret.colorDarker);
		ret.textColor = ColorPalette.textColor(ret.colorDarker);
		return ret;
	}

}
