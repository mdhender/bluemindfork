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
 * @fileoverview Three way merge algorithm for object.
 *
 * @author kyawtun@yathit.com (Kyaw Tun)
 */

goog.provide('ydn.object.merge');
goog.require('ydn.string.diff');


/**
 * Merge two object by using three-way merge.
 * This function does not modify input objects.
 * @param {Object} ver2 modified object of most recent version.
 * @param {Object} ver1 modified object.
 * @param {Object} orginal original object.
 * @return {Object} merged object.
 */
ydn.object.merge = function(ver2, ver1, orginal) {
  if (!ver1) {
    return ver2;
  }
  if (!ver2) {
    return ver1;
  }

  return ydn.object.merge_att_(ver2, ver1, orginal);

};


/**
 * Merge by attributes.
 * @param {Object} ver2 modified object of most recent version.
 * @param {Object} ver1 modified object.
 * @param {Object} original original object.
 * @return {Object} merged object.
 * @private
 */
ydn.object.merge_att_ = function(ver2, ver1, original) {
  var fields = goog.object.getKeys(original);
  for (var key in ver2) {
    if (ver2.hasOwnProperty(key)) {
      if (fields.indexOf(key) == -1) {
        fields.push(key);
      }
    }
  }
  for (var key in ver1) {
    if (ver1.hasOwnProperty(key)) {
      if (fields.indexOf(key) == -1) {
        fields.push(key);
      }
    }
  }
  var out = {};
  for (var i = 0; i < fields.length; i++) {
    var key = fields[i];
    if (ver2[key] === ver1[key]) { // no conflict
      out[key] = ver2[key];
    } else {
      var type2 = goog.typeOf(ver2[key]);
      var type1 = goog.typeOf(ver1[key]);
      var type0 = goog.typeOf(original[key]);
      if (type0 == type1 && type0 == type2) {
        if (type0 == 'number' || original[key] instanceof Date) {
          if (ver1[key] === original[key]) { // ver1 has no edit
            out[key] = ver2[key];
          } else if (ver2[key] === original[key]) { // ver2 has no edit
            out[key] = ver1[key];
          } else { // both has edit
            // ver2 always win for conflict
            out[key] = ver2[key];
          }
        } else if (type0 == 'string') {
          var str_merge_result = ydn.string.diff.diff3_merge(
              ver2[key].split(' '), original[key].split(' '),
              ver1[key].split(' '));
          var s = [];
          for (var j = 0; j < str_merge_result.length; j++) {
            var result = str_merge_result[j];
            if (result.ok) {
              Array.prototype.push.apply(s, result.ok);
            } else {
              Array.prototype.push.apply(s, result.conflict.a);
            }
          }
          out[key] = s.join(' ');
        } else {
          out[key] = ydn.object.merge_att_(ver2[key], ver1[key], original[key]);
        }
      } else if (type0 == 'undefined' && type1 == 'undefined') {
        out[key] = ver2[key];
      } else if (type0 == 'undefined' && type2 == 'undefined') {
        out[key] = ver1[key];
      }
    }
  }
  return out;
};
