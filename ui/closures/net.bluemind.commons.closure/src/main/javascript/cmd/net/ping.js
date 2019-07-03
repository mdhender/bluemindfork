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
 * Ping command.
 */

goog.provide('bluemind.cmd.net.Ping');

goog.require('bluemind.cmd.DeferredCommand');



/**
 * A ping command.
 * @param {string} path Application rpc base path
 * @param {goog.async.Deferred} deferred Deferred object that will propagate
 *   the success or failure.
 *
 * @constructor
 * @extends {bluemind.cmd.DeferredCommand}
 */
bluemind.cmd.net.Ping = function(path, deferred) {
  var uid = 'bluemind.cmd.net.Ping:' + goog.now();
  //TODO: kind of crappy
  goog.base(this, deferred, uid, path + '/ping');
};

goog.inherits(bluemind.cmd.net.Ping, 
  bluemind.cmd.DeferredCommand);

/** @override */
bluemind.cmd.net.Ping.prototype.onSuccess = function(event) {
  var xhr = event.target;
  var ts = xhr.getResponseText();
  this.callersOnSuccess(ts);
};

/** @override */
bluemind.cmd.net.Ping.prototype.onFailure = function(event) {
  this.callersOnFailure(event);
};
