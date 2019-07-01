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

goog.provide('net.bluemind.todolist.api.i18n.Classification.Caption');

/** @meaning tasks.classification.public */
net.bluemind.todolist.api.i18n.Classification.MSG_PUBLIC = goog
		.getMsg('Public');

/** @meaning tasks.classification.private */
net.bluemind.todolist.api.i18n.Classification.MSG_PRIVATE = goog
		.getMsg('Private');

/** @meaning tasks.classification.confidential */
net.bluemind.todolist.api.i18n.Classification.MSG_CONFIDENTIAL = goog
		.getMsg('Confidential');

/**
 * Constants for state names.
 * 
 * @enum {string}
 */
net.bluemind.todolist.api.VTodo.Classification = {
	'Public' : net.bluemind.todolist.api.i18n.Classification.MSG_PUBLIC,
	'Private' : net.bluemind.todolist.api.i18n.Classification.MSG_PRIVATE,
	'Confidential' : net.bluemind.todolist.api.i18n.Classification.MSG_CONFIDENTIAL,
};
