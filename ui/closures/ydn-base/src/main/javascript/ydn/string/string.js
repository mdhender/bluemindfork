// Copyright 2012 YDN Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS-IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


/**
 * @fileoverview Some utilities on top of goog.object module.
 *
 */

goog.provide('ydn.string');


/**
 * Split comma sperated tokens. Token can be possibly double quoted token.
 * @param {string} str string to split.
 * @return {Array.<string>} list of tokens.
 */
ydn.string.split_comma_seperated = function(str) {
  return str.match(/(?:"[^"]*"|[^,])+/g);
};


/**
 * Split string by space separated, possibly double quoted, string.
 * @param {string} str string to be split.
 * @return {Array.<string>} resulting split string.
 */
ydn.string.split_space_seperated = function(str) {
  return str.match(/\w+|"[^"]+"/g);
};


/**
 * Split string by space separated, possibly single or double quoted, string.
 * @param {string} str string to be split.
 * @return {Array.<string>} quote preserved tokens.
 */
ydn.string.split_space = function(str) {
  return str.match(/[^\s"']+|"[^"]*"|'[^']*'/g);
};


/**
 * Normalize phone number.
 * @param {string} ph
 * @return {?string} null if not a valid email.
 */
ydn.string.normalizePhone = function(ph) {
  if (!goog.isString(ph)) {
    return null;
  }
  return ph.replace(/\D/g, '').replace(/^0+/, '');
};


/**
 * Normalize email address.
 * @param {string|*} email
 * @return {?string} null if not a valid email.
 */
ydn.string.normalizeEmail = function(email) {
  if (!goog.isString(email)) {
    return null;
  }
  if (!/.+@.+\..+/.test(email)) {
    return null;
  }
  // remove name from email label, such as 'brandi steel <brandiengage@gmail.com>'
  email = email.replace(/.*</, '').replace(/>.*/, '');
  email = email.toLowerCase();
  // remove + and . before @ sign
  // http://gmailblog.blogspot.sg/2008/03/2-hidden-ways-to-get-more-from-your.html
  email = email.replace(/\+\w*?@/, '@'); // normalize for gmail using +
  // should we be using regexp?
  for (var i = email.indexOf('@'); i >= 0; i--) {
    if (email.charAt(i) == '.') {
      email.slice(i, 1);
    }
  }
  return email;
};


/**
 * Join list of string by comma and lastly by word 'and'.
 * @param {Array.<string>} items
 * @return {string} string joining all items by comma separated and and.
 */
ydn.string.joinWithCommaAnd = function(items) {
  var n = items.length;
  if (n == 0) {
    return '';
  } else if (n == 1) {
    return items[0];
  } else if (n == 2) {
    return items[0] + ' and ' + items[1];
  } else {
    var city_name = items.slice(0, n - 2).join(', ');
    return city_name + ', ' + items[n - 2] + ' and ' + items[n - 1];
  }
};


/**
 * Converts an ArrayBuffer directly to base64, without any intermediate
 * 'convert to string then use window.btoa' step. According to my tests, this
 * appears to be a faster approach:
 * http://jsperf.com/encoding-xhr-image-data/5
 * https://gist.github.com/jonleighton/958841
 * @param {ArrayBuffer} arrayBuffer
 * @return {string}
 */
ydn.string.encodeArrayBuffer2base64 = function(arrayBuffer) {
  var base64 = '';
  var encodings = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

  var bytes = new Uint8Array(arrayBuffer);
  var byteLength = bytes.byteLength;
  var byteRemainder = byteLength % 3;
  var mainLength = byteLength - byteRemainder;

  var a, b, c, d;
  var chunk;

  // Main loop deals with bytes in chunks of 3
  for (var i = 0; i < mainLength; i = i + 3) {
    // Combine the three bytes into a single integer
    chunk = (bytes[i] << 16) | (bytes[i + 1] << 8) | bytes[i + 2];

    // Use bitmasks to extract 6-bit segments from the triplet
    a = (chunk & 16515072) >> 18; // 16515072 = (2^6 - 1) << 18;
    b = (chunk & 258048) >> 12; // 258048   = (2^6 - 1) << 12;
    c = (chunk & 4032) >> 6; // 4032     = (2^6 - 1) << 6;
    d = chunk & 63;               // 63       = 2^6 - 1;

    // Convert the raw binary segments to the appropriate ASCII encoding
    base64 += encodings[a] + encodings[b] + encodings[c] + encodings[d];
  }

  // Deal with the remaining bytes and padding
  if (byteRemainder == 1) {
    chunk = bytes[mainLength];

    a = (chunk & 252) >> 2; // 252 = (2^6 - 1) << 2

    // Set the 4 least significant bits to zero
    b = (chunk & 3) << 4; // 3   = 2^2 - 1

    base64 += encodings[a] + encodings[b] + '==';
  } else if (byteRemainder == 2) {
    chunk = (bytes[mainLength] << 8) | bytes[mainLength + 1];

    a = (chunk & 64512) >> 10; // 64512 = (2^6 - 1) << 10
    b = (chunk & 1008) >> 4; // 1008  = (2^6 - 1) << 4

    // Set the 2 least significant bits to zero
    c = (chunk & 15) << 2; // 15    = 2^4 - 1

    base64 += encodings[a] + encodings[b] + encodings[c] + '=';
  }

  return base64;
};


/**
 * Returns the non-overlapping occurrences of ss in reg.
 * @param {string} ss
 * @param {RegExp} reg
 * @return {number} occurrences.
 */
ydn.string.countByReg = function(ss, reg) {
  // a first way to count occurrences.
  if (!ss) {
    return 0;
  }
  var m = ss.match(reg);
  return m ? m.length : 0;
};


/**
 * Count number of words.
 * @param {string} ss
 * @return {number}
 */
ydn.string.wordCount = function(ss) {
  return ydn.string.countByReg(ss, /\w+/g);
};


/**
 * Returns the non-overlapping occurrences of ss in t.
 * @param {string} ss
 * @param {string} t
 * @return {number} occurrences of t.
 */
ydn.string.countToken = function(ss, t) {
  return ydn.string.countByReg(ss, new RegExp(ss, 'g'));
};


/**
 * Try parse string to number.
 * @param {string} val
 * @return {number} NaN return if fail to parse.
 */
ydn.string.tryParse = function(val) {
  try {
    return parseInt(val, 10);
  } catch (e) {
    return NaN;
  }
};

