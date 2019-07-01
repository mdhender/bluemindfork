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
 * @fileoverview Generator for unique IDs.
 *
 */

goog.provide('bluemind.IdGenerator');

/**
 * Creates a new id generator.
 * @constructor
 */
bluemind.IdGenerator = function() {
};

/**
 * The simplest function to get an UUID string.
 * @returns {string} A version 4 UUID string.
 */
bluemind.IdGenerator.generate = function() {
  var uuid = bluemind.IdGenerator.pad_(bluemind.IdGenerator.random_(32), 8)
    + "-" + bluemind.IdGenerator.pad_(bluemind.IdGenerator.random_(16), 4)
    + "-" 
    + bluemind.IdGenerator.pad_(0x4000 | bluemind.IdGenerator.random_(12), 4)
    + "-"
    + bluemind.IdGenerator.pad_(0x8000 | bluemind.IdGenerator.random_(14), 4)
    + "-" + bluemind.IdGenerator.pad_(bluemind.IdGenerator.random_(48), 12);
  return uuid;
};

/**
 * Returns an unsigned x-bit random integer.
 * @param {number} x A positive integer ranging from 0 to 53, inclusive.
 * @returns {number} An unsigned x-bit random integer (0 <= f(x) < 2^x).
 * @private
 */
bluemind.IdGenerator.random_= function(x) {
  if (x <= 30) return (0 | Math.random() * (1 << x));
  return bluemind.IdGenerator.random_(30) 
    + (0 | Math.random() * (1 << x - 30)) * (1 << 30);
};

/**
 * Converts an integer to a zero-filled string.
 * @param {number} num Number to pad.
 * @param {number} length String desired length.
 * @returns {string} Number filled with 0.
 */
bluemind.IdGenerator.pad_ = function(num, length) {
  var str = num.toString(16)
  var i = length - str.length
  var z = "0";
  for (; i > 0; i >>>= 1, z += z) { if (i & 1) { str = z + str; } }
  return str;
};

