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
 * @fileoverview Manage folder storage.
 */

goog.provide("net.bluemind.authentication.api.AuthClient");
goog.require("net.bluemind.api.BlueMindClient");
goog.require("relief.rpc.Command");

/**
 * Authentication client
 * 
 * @param {relief.rpc.RPCService} rpc RPC Service
 * @param {string} base Base url
 * @constructor
 * @extends {net.bluemind.api.BlueMindClient}
 */
net.bluemind.authentication.api.AuthClient = function(rpc, base) {
  goog.base(this, rpc, base);
};
goog.inherits(net.bluemind.authentication.api.AuthClient, net.bluemind.api.BlueMindClient);

/**
 * Get Current user
 * 
 * @return {goog.Thenable}
 */
net.bluemind.authentication.api.AuthClient.prototype.getCurrentUser = function() {
  var url = this.base + "/auth";
  var cmd = new relief.rpc.Command(null, null, "id", url, "GET");
  return this.execute(cmd, null);
};

/**
 * Get user settings
 * 
 * @param {string} domain User domain
 * @param {string} userId User uid
 * @return {goog.Thenable}
 */
net.bluemind.authentication.api.AuthClient.prototype.getSettings = function(domain, userId) {
  var url = this.base + "/users/" + domain + "/" + userId + "/_settings";
  var cmd = new relief.rpc.Command(null, null, "retrieve_settings", url, "GET");
  return this.execute(cmd, null);
}
