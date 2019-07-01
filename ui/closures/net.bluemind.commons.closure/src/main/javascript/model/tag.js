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

goog.provide('bluemind.model.Tag');

goog.require('goog.math');

/**
 * A Tag is used to categorize or to summurize the content of an object.
 * 
 * @constructor
 */
bluemind.model.Tag = function() {
  var i = goog.math.randomInt(bluemind.model.Tag.COLORS.length);
  this.color = bluemind.model.Tag.COLORS[i];
};

/**
 * Tag remote unique identifier
 * 
 * @type {string}
 * @private
 */
bluemind.model.Tag.prototype.id;

/**
 * Tag remote container identifier
 * 
 * @type {string}
 * @private
 */
bluemind.model.Tag.prototype.container;

/**
 * Tag label
 * 
 * @type {string}
 * @private
 */
bluemind.model.Tag.prototype.label;

/**
 * Color associated with the tag. This color is either the default color of the
 * tag or the personnal color it defined.
 * 
 * @type {string}
 * @private
 */
bluemind.model.Tag.prototype.color;

bluemind.model.Tag.COLORS = [ "e7a1a2", "f9ba89", "f7dd8f", "fcfa90", "78d168", "9fdcc9", "c6d2b0", "9db7e8", "b5a1e2",
    "daaec2", "dad9dc", "6b7994", "bfbfbf", "6f6f6f", "4f4f4f", "c11a25", "e2620d", "c79930", "b9b300", "368f2b",
    "329b7a", "778b45", "2858a5", "5c3fa3", "93446b" ];
