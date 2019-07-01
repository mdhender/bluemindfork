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

goog.provide('ydn.object');
goog.require('ydn.json');


/**
 * @define {boolean} debug flat.
 */
ydn.object.DEBUG = false;


/**
 * Deep object cloning by JSON stringify/parse
 *
 * @param {T} any entity to be cloned.
 * @return {T} cloned entity.
 * @template T
 */
ydn.object.clone = function(any) {
  if (typeof any == 'undefined') {
    return undefined;
  }
  if (typeof any == 'number' && isNaN(any)) {
    return NaN;
  }
  return ydn.json.parse(ydn.json.stringify(any));
};


/**
 * Deeply compare two objects or arrays.
 *
 * @param {*} obj1 object 1 to be compare with.
 * @param {*} obj2 object 2 to be compare with.
 * @param {Object=} opt_ignore_fields optional field to be ignore in comparing.
 * @return {boolean} true if same contents.
 */
ydn.object.equals = function(obj1, obj2, opt_ignore_fields) {
  opt_ignore_fields = opt_ignore_fields || {};
  if (!goog.isDefAndNotNull(obj1) || !goog.isDefAndNotNull(obj2)) {
    if (ydn.object.DEBUG) {
      if (!goog.isDefAndNotNull(obj1)) {
        goog.global.console.log('obj1 is not defined.');
      }
      if (!goog.isDefAndNotNull(obj2)) {
        goog.global.console.log('obj2 is not defined.');
      }
    }
    return false;
  } else if (goog.isArrayLike(obj1) && goog.isArrayLike(obj2)) {
    var arr1 = obj1;
    var arr2 = /** @type {Array} */ (obj2);
    if (obj1.length != obj2.length) {
      if (ydn.object.DEBUG) {
        goog.global.console.log('different array length ' + obj1.length + ' vs ' + obj2.length);
      }
      return false;
    }
    for (var i = 0; i < obj1.length; i++) {
      var idx = goog.array.findIndex(arr2, function(ele) {
        return ydn.object.equals(ele, arr1[i]);
      });
      if (idx == -1) {
        if (ydn.object.DEBUG) {
          goog.global.console.log('obj2 do not have ' + obj1[i]);
        }
        return false;
      }
    }
    return true;
  } else if (goog.isArrayLike(obj1)) {
    return obj1.length == 1 && ydn.object.equals(obj1[0], obj2);
  } else if (goog.isArrayLike(obj2)) {
    return obj2.length == 1 && ydn.object.equals(obj2[0], obj1);
  } else if (goog.isObject(obj1) && goog.isObject(obj1)) {
    for (var key in obj1) {
      if (obj1.hasOwnProperty(key) && !opt_ignore_fields[key]) {
        var same = ydn.object.equals(obj1[key], obj2[key]);
        if (!same) {
          if (ydn.object.DEBUG) {
            goog.global.console.log('value for ' + key);
          }
          return false;
        }
      }
    }
    for (var key in obj2) {
      if (obj2.hasOwnProperty(key) && !opt_ignore_fields[key]) {
        var same = ydn.object.equals(obj1[key], obj2[key]);
        if (!same) {
          if (ydn.object.DEBUG) {
            goog.global.console.log('value for ' + key);
          }
          return false;
        }
      }
    }
    return true;
  } else {
    return obj1 === obj2;
  }
};


/**
 * Length of array-like object. If it define {@code length} number field, it
 * return that value.
 *
 * @param {Object} obj object of concern.
 * @return {number} return number of hasOwnProperty fields.
 */
ydn.object.length = function(obj) {
  if (obj && obj['length'] && goog.isNumber(obj['length'])) {
    return obj.length;
  }
  var count = 0;
  for (var id in obj) {
    if (obj.hasOwnProperty(id)) {
      count++;
    }
  }
  return count;
};


/**
 * @see {@link goog.object.extend}
 * @param {Object} target  The source object.
 * @param {...Object} var_args The objects from which values will be copied.
 * @return {Object} extended object.
 */
ydn.object.extend = function(target, var_args) {
  var out = ydn.object.clone(target);
  for (var key in var_args) {
    if (var_args.hasOwnProperty(key)) {
      out[key] = var_args[key];
    }
  }
  return out;
};


/**
 * Construct array of having value v.
 * @param {*} v element value.
 * @param {number} n number of items.
 * @return {!Array} array preallocated with value v.
 */
ydn.object.reparr = function(v, n) {
  // this is the most efficient way in chrome.
  // IE10 can be more efficient by preallocating.
  // new Array(n); // preallocating.
  var arr = [];
  for (var i = 0; i < n; i++) {
    arr[i] = v;
  }
  return arr;
};


/**
 * Take the first field of an object
 * @final
 * @param {*} row row.
 * @return {*} the first field of object in row value.
 */
ydn.object.takeFirst = function(row) {
  if (!row) {
    return;
  }
  for (var key in row) {
    if (row.hasOwnProperty(key)) {
      return row[key];
    }
  }
  return undefined;
};
