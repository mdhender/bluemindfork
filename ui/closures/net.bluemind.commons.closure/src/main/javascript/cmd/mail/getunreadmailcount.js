/**
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
 * @fileoverview
 *
 * Create or update a contact.
 */

goog.provide('bluemind.cmd.mail.GetUnreadMailCount');

goog.require('bluemind.cmd.DeferredCommand');



/**
 * A command object for deleting contact details.
 * @param {string} path Application rpc base path
 * @param {goog.async.Deferred} deferred Deferred object that will propagate
 *   the success or failure.
 *
 * @constructor
 * @extends {bluemind.cmd.DeferredCommand}
 */
bluemind.cmd.mail.GetUnreadMailCount = function(path, deferred) {
  var uid = 'bluemind.cmd.mail.GetUnreadMailCount:' + goog.now();
  goog.base(this, deferred, uid, path + '/bmc', 'user', 'getUnreadMailCount');
};

goog.inherits(bluemind.cmd.mail.GetUnreadMailCount, 
  bluemind.cmd.DeferredCommand);

/** @override */
bluemind.cmd.mail.GetUnreadMailCount.prototype.onSuccess = function(event) {
  var xhr = event.target;
  var mail = JSON.parse(xhr.getResponseText());
  this.callersOnSuccess(mail['unread']);
};

/** @override */
bluemind.cmd.mail.GetUnreadMailCount.prototype.onFailure = function(event) {
  this.callersOnFailure(event);
};
