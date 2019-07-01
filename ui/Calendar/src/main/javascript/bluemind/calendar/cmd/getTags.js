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

goog.provide('bluemind.calendar.cmd.GetTags');

goog.require('bluemind.cmd.DeferredCommand');
goog.require('bluemind.model.Tag');



/**
 * A command object for getting user's tag list.
 * @param {goog.async.Deferred} deferred Deferred object that will propagate
 *   the success or failure.
 * @constructor
 * @extends {bluemind.cmd.DeferredCommand}
 */
bluemind.calendar.cmd.GetTags = function(deferred) {
  var uid = 'bluemind.calendar.cmd.GetTags:' + goog.now();
  goog.base(this, deferred, uid, 'calendar/bmc', 'user', 'getTags');
};

goog.inherits(bluemind.calendar.cmd.GetTags, 
  bluemind.cmd.DeferredCommand);

/** @override */
bluemind.calendar.cmd.GetTags.prototype.onSuccess =
  function(event) {
  var xhr = event.target;
  var r = JSON.parse(xhr.getResponseText());
  var tags = [];
  for (var i = 0; i < r['tags'].length; i++) {
    var tag = bluemind.model.Tag.parse(r['tags'][i]);
    tags.push(tag);
  }
  this.callersOnSuccess(tags);
};

/** @override */
bluemind.calendar.cmd.GetTags.prototype.onFailure =
  function(event) {
  this.callersOnFailure(event);
};
