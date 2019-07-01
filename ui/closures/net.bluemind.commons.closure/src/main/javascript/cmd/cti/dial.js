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
 * A command object for getting tag's list .
 */

goog.provide('bluemind.cmd.cti.Dial');

goog.require('bluemind.cmd.DeferredCommand');



/**
 * A command object for dialing a phone number.
 * @param {string} path Application rpc base path
 * @param {goog.async.Deferred} deferred Deferred object that will propagate
 *   the success or failure.
 * @constructor
 * @extends {bluemind.cmd.DeferredCommand}
 */
bluemind.cmd.cti.Dial = function(path, number, deferred) {
  var uid = 'bluemind.cmd.cti.Dial:' + goog.now();
  goog.base(this, deferred, uid, path + '/bmc', 'cti', 'dial');
  this.data.add('number', number);
};

goog.inherits(bluemind.cmd.cti.Dial, bluemind.cmd.DeferredCommand);

/** @override */
bluemind.cmd.cti.Dial.prototype.onSuccess =
function(event) {
  var xhr = event.target;
  try {
    var r = JSON.parse(xhr.getResponseText());
  } finally { 
    if (!r || r['error']) {
      this.callersOnFailure(event);
    } else {
      this.callersOnSuccess(r['number']);
    }
  }
};

/** @override */
bluemind.cmd.cti.Dial.prototype.onFailure =
function(event) {
  this.callersOnFailure(event);
};

