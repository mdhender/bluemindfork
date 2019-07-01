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
*/


/**
 * @fileoverview Provides a convenient API for data persistence with a 2 steps
 * storage mechanism.
 *
 */

goog.provide('bluemind.storage.ObjectStorage');

goog.require('bluemind.storage.Storage');
goog.require('goog.json');
goog.require('goog.json.Serializer');
goog.require('goog.storage.ErrorCode');
goog.require('goog.storage.mechanism.Mechanism');



/**
 * The base implementation for storing object.
 *
 * @param {!goog.storage.mechanism.Mechanism} mechanism The underlying
 *     storage mechanism.
 * @constructor
 * @extends {bluemind.storage.Storage} 
 */
bluemind.storage.ObjectStorage = function(mechanism) {
  goog.base(this, mechanism);
};
goog.inherits(bluemind.storage.ObjectStorage, bluemind.storage.Storage);


/** @override */
bluemind.storage.ObjectStorage.prototype.set = function(key, value) {
  if (!goog.isDef(value)) {
    this.mechanism.remove(key);
  } else {
    this.mechanism.set(key, /** @type {string} */ (value));
  }
};


/** @override */
bluemind.storage.ObjectStorage.prototype.get = function(key) {
  var data = this.mechanism.get(key);
  if (goog.isNull(data)) {
    return undefined;
  }
  return data;
};
