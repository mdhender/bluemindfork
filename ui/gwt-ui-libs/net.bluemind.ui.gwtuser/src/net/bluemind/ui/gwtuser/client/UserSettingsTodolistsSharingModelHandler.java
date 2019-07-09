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
package net.bluemind.ui.gwtuser.client;

import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.todolist.api.ITodoUids;

public class UserSettingsTodolistsSharingModelHandler extends BaseCanManageSharingsModelHandler {

	public static final String TYPE = "bm.settings.UserTodolistsSharingModelHandler";

	public UserSettingsTodolistsSharingModelHandler() {
		super(ITodoUids.TYPE, "tasks-sharing");
	}

	public static void registerType() {
		GwtModelHandler.register(TYPE,
				new IGwtDelegateFactory<IGwtModelHandler, net.bluemind.gwtconsoleapp.base.editor.ModelHandler>() {

					@Override
					public IGwtModelHandler create(net.bluemind.gwtconsoleapp.base.editor.ModelHandler model) {
						return new UserSettingsTodolistsSharingModelHandler();
					}
				});
	}
}
