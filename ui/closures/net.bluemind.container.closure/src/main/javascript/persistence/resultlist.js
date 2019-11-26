/* BEGIN LICENSE
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

goog.provide('net.bluemind.container.persistence.ResultList');

/**
 * Set of contact
 * 
 * @param {number} count
 * @param {Array} entries
 * @constructor
 */
net.bluemind.container.persistence.ResultList = function(count, entries) {
  this.count_ = count;
  this.items_ = entries;
};

net.bluemind.container.persistence.ResultList.PAGE = 200;
/**
 * Length of the result
 * 
 * @type {number}
 * @private
 */
net.bluemind.container.persistence.ResultList.prototype.count_;

/**
 * entries
 * 
 * @type {Array}
 * @private
 */
net.bluemind.container.persistence.ResultList.prototype.items_;

/**
 * Returns an array containing all the elements in this set.
 * 
 * @return {Array} An array containing all the elements in this set.
 */
net.bluemind.container.persistence.ResultList.prototype.getItems = function() {
  return this.items_;
};

/**
 * 
 * @return {number}
 */
net.bluemind.container.persistence.ResultList.prototype.getCount = function() {
  return this.count_;
};
