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
package net.bluemind.ui.adminconsole.dataprotect;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.core.api.VersionInfo;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.ui.common.client.forms.TrPanel;
import net.bluemind.ui.common.client.icon.Trash;

public class GenerationRow extends TrPanel {

	public GenerationRow(DataProtectGeneration dpg, VersionInfo currentVersion, boolean licensePresent) {
		boolean restoreAllowed = true;
		if (!licensePresent && dpg.blueMind != null && !dpg.blueMind.equals(currentVersion)) {
			restoreAllowed = false;
		}

		Label img = new Label();
		img.setStyleName("fa fa-clock-o fa-lg");
		add(img);
		String title = dpg.protectionTime != null ? format(dpg.protectionTime) : "gen " + dpg.id;

		if (restoreAllowed) {
			Anchor genLink = new Anchor(title);
			add(genLink);
			genLink.addClickHandler(new LoadGenHandler(dpg));
		} else {
			add(new Label(title));
		}

		if (dpg.blueMind != null) {
			Label version = new Label("v" + dpg.blueMind.toString());
			if (!restoreAllowed) {
				version.setStylePrimaryName("red");
			}
			add(version);
		} else {
			add(new Label("Unknown version"));
		}
		BulletList content = new BulletList();
		long total = 0;
		for (PartGeneration pp : dpg.parts) {
			long size = pp.size;
			content.add(new Label(pp.tag + " " + SizeFormat.readableFileSize(size)));
			total += size;
		}
		add(content);
		add(new Label(SizeFormat.readableFileSize(total)));

		Trash trash = new Trash();
		add(trash, "trash");
		trash.addClickHandler(new ForgetGenHandler(dpg));
	}

	private String format(Date d) {
		return DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_MEDIUM).format(d);
	}

}
