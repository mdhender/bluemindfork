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
 * @fileoverview Robust JSON wrapper.
 */

goog.provide('ydn.json');
goog.require('goog.json');


/**
 *
 * @define {boolean} enable debug to log erronous object.
 */
ydn.json.DEBUG = false;


/**
 * @define {boolean} Use JSON polyfill.
 */
ydn.json.POLY_FILL = false;


/**
 * Parse JSON using native method if available.
 * This is necessary since closure-library do not use native method.
 *
 * @param {*} json_str string to parse.
 * @return {!Object} parse return object.
 */
ydn.json.parse = function(json_str) {
  if (!goog.isString(json_str) || goog.string.isEmpty(json_str)) {
    return {};
  }
  if (ydn.json.POLY_FILL && (typeof goog.global['JSON'] == 'undefined')) {
    return /** @type {!Object} */ (goog.json.unsafeParse(json_str));
  } else {
    return /** @type {!Object} */ (JSON.parse(json_str));
  }
};


/**
 * @param {string} s
 * @return {*}
 */
ydn.json.tryParse = function(s) {
  if (goog.isString(s) && !goog.string.isEmpty(s)) {
    var json;
    /** @preserveTry */
    try {
      json = JSON.parse(s);
    } catch (e) {
      json = null;
    }
    return json;
  } else {
    return undefined;
  }
};


/**
 * Note: This is mainly used in debugging.
 * @param obj
 * @return {string}
 */
ydn.json.toShortString = function(obj) {
  var json;
  /** @preserveTry */
  try {
    json = goog.isString(obj) ? obj : ydn.json.stringify(obj);
  } catch (e) {
    json = '';
  }
  if (json) {
    return json.substr(0, 70) + (json.length > 70 ? '...' : '');
  } else {
    return '';
  }

};


/**
 * Parse JSON using native method if available.
 *
 * This is necessary since closure-library do not use native method.
 *
 * @param {Object} json object to stringify.
 * @param {Function=} opt_replacer If a function, transforms values and properties
 * encountered while stringifying.
 * @param {number=} opt_space Causes the resulting string to be pretty-printed.
 * @return {string} result.
 */
ydn.json.stringify = function(json, opt_replacer, opt_space) {

  if (ydn.json.POLY_FILL && (typeof goog.global['JSON'] == 'undefined')) {
    return goog.json.serialize(json, opt_replacer);
  } else {
    return JSON.stringify(json, opt_replacer, opt_space);
  }

};
