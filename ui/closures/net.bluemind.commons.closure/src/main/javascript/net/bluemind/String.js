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
 * @fileoverview Utilities for string manipulation.
 */

/**
 * Namespace for string utilities
 */
goog.provide("bluemind.string");
goog.provide("net.bluemind.string");

/**
 * Normalizes accent in a string, replacing all accented chars with the
 * unaccented version.
 * 
 * @param {string} str The string in which to normalize accent.
 * @return {string} A copy of {@code str} with all accented charsnormalized.
 */
net.bluemind.string.normalizeAccent = function(str) {
  var pattern = /[öäüÖÄÜáàâéèêúùûóòôÁÀÂÉÈÊÚÙÛÓÒÔß]/g;

  var normalizeTable = {
    "ä" : "a",
    "ö" : "o",
    "ü" : "u",
    "Ä" : "A",
    "Ö" : "O",
    "Ü" : "U",
    "á" : "a",
    "à" : "a",
    "â" : "a",
    "é" : "e",
    "è" : "e",
    "ê" : "e",
    "ú" : "u",
    "ù" : "u",
    "û" : "u",
    "ó" : "o",
    "ò" : "o",
    "ô" : "o",
    "Á" : "A",
    "À" : "A",
    "Â" : "A",
    "É" : "E",
    "È" : "E",
    "Ê" : "E",
    "Ú" : "U",
    "Ù" : "U",
    "Û" : "U",
    "Ó" : "O",
    "Ò" : "O",
    "Ô" : "O",
    "ß" : "s"
  };
  var translator = function(match) {
    return normalizeTable[match] || match;
  }
  return str.replace(pattern, translator);
};

/**
 * Normalizes string into a comparable version.
 * 
 * @param {string} str The string in which to normalize.
 * @return {string} A copy of {@code str} with all chars normalized.
 */
net.bluemind.string.normalize = function(str) {
  var s = net.bluemind.string.normalizeAccent(str.toLowerCase());
  return s;
}

/**
 * A string comparator that ignores case and accent.
 * 
 * <pre>
 * -1 = str1 less than str2
 *  0 = str1 equals str2
 *  1 = str1 greater than str2
 * </pre>
 * 
 * @param {string} str1 The string to compare.
 * @param {string} str2 The string to compare {@code str1} to.
 * @return {number} The comparator result, as described above.
 */
net.bluemind.string.normalizedCompare = function(str1, str2) {
  var test1 = net.bluemind.string.normalizeAccent(str1);
  var test2 = net.bluemind.string.normalizeAccent(str2);

  return goog.string.caseInsensitiveCompare(test1, test2);
}

bluemind.string = net.bluemind.string;
