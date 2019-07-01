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

import java.util.HashMap;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import net.bluemind.core.task.api.TaskRef;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.gwt.endpoint.DataProtectGwtEndpoint;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.common.client.forms.Ajax;

public class LoadGenHandler implements net.bluemind.core.api.AsyncHandler<TaskRef>, ClickHandler {

	private DataProtectGeneration dpg;

	public LoadGenHandler(DataProtectGeneration dpg) {
		this.dpg = dpg;
	}

	@Override
	public void onClick(ClickEvent event) {
		GWT.log("loadGenHandler");
		// Ajax.bmc.getContent(Ajax.TOKEN, dpg, this);
		DataProtectGwtEndpoint dpApi = new DataProtectGwtEndpoint(Ajax.TOKEN.getSessionId());
		dpApi.getContent("" + dpg.id, this);
	}

	@Override
	public void success(TaskRef result) {
		GWT.log("load success !!!");
		// ScreenShowRequest ssr = new ScreenShowRequest();
		// ssr.put("generation", dpg);
		// ssr.put("task", result);
		// // Send a List of ImageResource to add pictures
		// ssr.put("pictures", null);
		// ssr.put("return", "dpNavigator");
		// ssr.put("success", "dpGenBrowser");
		HashMap<String, String> ssr = new HashMap<>();
		ssr.put("generation", dpg.id + "");
		ssr.put("task", result.id + "");
		// Send a List of ImageResource to add pictures
		ssr.put("pictures", null);
		ssr.put("return", "dpNavigator");
		ssr.put("success", "dpGenBrowser");
		Actions.get().showWithParams2("progress", ssr);
	}

	@Override
	public void failure(Throwable e) {
	}
}
