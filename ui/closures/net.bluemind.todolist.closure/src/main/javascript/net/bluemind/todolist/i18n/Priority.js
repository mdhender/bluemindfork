/*
 * BEGIN LICENSE
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

goog.provide('net.bluemind.todolist.api.i18n.Priority.Caption');

/** @meaning tasks.priority.low */
net.bluemind.todolist.api.i18n.Priority.MSG_LOW = goog.getMsg('Low');
/** @meaning tasks.priority.medium */
net.bluemind.todolist.api.i18n.Priority.MSG_MEDIUM = goog.getMsg('Medium');
/** @meaning tasks.priority.high */
net.bluemind.todolist.api.i18n.Priority.MSG_HIGH = goog.getMsg('High');

/**
 * Constants for priority names.
 * 
 * @enum {number}
 */
net.bluemind.todolist.api.i18n.Priority.Caption = {
	9 : net.bluemind.todolist.api.i18n.Priority.MSG_LOW,
	5 : net.bluemind.todolist.api.i18n.Priority.MSG_MEDIUM,
	1 : net.bluemind.todolist.api.i18n.Priority.MSG_HIGH
};
