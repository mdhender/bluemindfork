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

/**
 * @fileoverview Task model.
 */

goog.provide('net.bluemind.todolist.api.i18n.Status.Caption');

/** @meaning tasks.status.needsAction */
net.bluemind.todolist.api.i18n.Status.MSG_NEEDSACTION = goog
		.getMsg('Not started');
/** @meaning tasks.status.completed */
net.bluemind.todolist.api.i18n.Status.MSG_COMPLETED = goog.getMsg('Completed');
/** @meaning tasks.status.inProgress */
net.bluemind.todolist.api.i18n.Status.MSG_INPROCESS = goog
		.getMsg('In progress');
/** @meaning tasks.status.cancelled */
net.bluemind.todolist.api.i18n.Status.MSG_CANCELLED = goog.getMsg('Cancelled');

/**
 * Constants for state names.
 * 
 * @enum {*}
 */
net.bluemind.todolist.api.i18n.Status.Caption = {
	'NeedsAction' : net.bluemind.todolist.api.i18n.Status.MSG_NEEDSACTION,
	'Completed' : net.bluemind.todolist.api.i18n.Status.MSG_COMPLETED,
	'InProcess' : net.bluemind.todolist.api.i18n.Status.MSG_INPROCESS,
	'Cancelled' : net.bluemind.todolist.api.i18n.Status.MSG_CANCELLED
};