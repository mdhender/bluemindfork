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
 * DeferredCommand is an abstract class extending RPC Command.
 * It use the deferred object to delegate onSuccess and onFailure
 * management.
 *
 */

goog.provide('bluemind.cmd.DeferredCommand');

goog.require('bluemind.cmd.Command');
goog.require('goog.async.Deferred');

/**
 * A abstract command object with onsuccess/onfailure delegated to an external
 * object.
 * @param {goog.async.Deferred} deferred Deferred object that will propagate
 *   the success or failure.
 * @param {string} commandID The ID for this command.
 * @param {string} url The URL to request.
 * @param {string=} opt_service Requested servlet.
 * @param {string=} opt_action The method to execute on the server side.
 * @param {number=} opt_maxRetries The number of retries.
 * @constructor
 * @extends {bluemind.cmd.Command}
 */
bluemind.cmd.DeferredCommand = function(deferred, commandID, url, opt_service,
  opt_action, opt_maxRetries) {
  this.deferred_ = deferred;
  goog.base(this, this.success_, this.failure_, commandID, url, opt_service,
    opt_action, opt_maxRetries);
};
goog.inherits(bluemind.cmd.DeferredCommand, bluemind.cmd.Command);

/**
 * Deferred object that will hold the success/failure logic
 * @type {goog.async.Deferred}
 * @private
 */
bluemind.cmd.DeferredCommand.prototype.deferred_;

/**
 * On success request pass the result to the deferred object and call
 * the attached callbacks
 * @param {*} result Request result
 */
bluemind.cmd.DeferredCommand.prototype.success_ = function(result) {
  this.deferred_.callback(result);
};

/**
 * On failure request pass the result to the deferred object and call
 * the attached errback
 * @param {*} result Request result
 */
bluemind.cmd.DeferredCommand.prototype.failure_ = function(result) {
  this.deferred_.errback(result);
};
