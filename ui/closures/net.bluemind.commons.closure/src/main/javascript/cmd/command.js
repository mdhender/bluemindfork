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
 * Command is an abstract class extending RPC Command.
 * It add additionals parameters like service and method to match the bluemind
 * request structure.
 * 
 */


goog.provide('bluemind.cmd.Command');

goog.require('goog.Uri.QueryData');
goog.require('relief.rpc.Command');
goog.require('goog.json.Serializer');


/**
 * Command for requesting bluemind server.
 * @param {Function} onSuccess Called on a successful response.
 * @param {Function} onFailure Called if the request fails.
 * @param {string} commandID The ID for this command.
 * @param {string} url The URL to request.
 * @param {string=} opt_service Requested servlet.
 * @param {string=} opt_action The method to execute on the server side.
 * @param {number=} opt_maxRetries The number of retries.
 * @constructor
 * @extends {relief.rpc.Command}
 */
bluemind.cmd.Command = function(onSuccess, onFailure, commandID, url, opt_service,
  opt_action, opt_maxRetries) {
  this.data = new goog.Uri.QueryData();
  if (opt_service) this.data.add('service', opt_service);
  if (opt_action) this.data.add('method', opt_action);
  this.serializer = new goog.json.Serializer();
  goog.base(this, onSuccess, onFailure, commandID, url, 'POST',
    opt_maxRetries);
};
goog.inherits(bluemind.cmd.Command, relief.rpc.Command);

/**
 * Post content
 * @type {goog.Uri.QueryData}
 * @protected
 */
bluemind.cmd.Command.prototype.data;

/**
 * The JSON serializer used to serialize values.
 *
 * @type {goog.json.Serializer}
 * @protected
 */
bluemind.cmd.Command.prototype.serializer = null;

/** @override */
bluemind.cmd.Command.prototype.getData = function() {
  return this.data.toString();
};
