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
 * @fileoverview Serializer for formating object into the local storage.
 */

goog.provide('bluemind.storage.Serializer');
goog.provide('bluemind.storage.Serializer.Type');

/**
 * Format object into a object that fit into the local storage.
 * Properties names are always transformed to be smaller.
 * Values are compressed when possible.
 * The algorithm that transform keys is very simple, the first property will be
 * 'A', then next one wil be 'B',... . When unserilizing, the first property
 * getted will be 'A'... . So you must set and get property into the same
 * order.
 *
 * @constructor
 */
bluemind.storage.Serializer = function() {
};

/**
 * Compress an objet.
 * @param {Object} object Object to serialize.
 * @param {Object.<String, {kind, def?, values?, item?}>} schema Object schema
 * @return {Object} Serialized object.
 */
bluemind.storage.Serializer.prototype.serialize = function(object, schema) {
  var serialized = {};
  var k = 'A'.charCodeAt(0);
  for (var key in schema) {
    var compressedKey = String.fromCharCode(k++);
    var equals = (object[key] == schema[key].def);
    equals = equals || goog.array.equals(object[key], schema[key].def);
    if (!equals) {
      var compressedValue = this.compressValue_(object[key], schema[key]);
      serialized[compressedKey] = compressedValue;
    }
  }
  return serialized;
};

/**
 * Compress an objet.
 * @param {Object} object Serialized object.
 * @param {Object.<String, {kind, def?, values?, item?}>} schema Object schema
 * @return {Object} unserialized object.
 */
bluemind.storage.Serializer.prototype.unserialize = function(object, schema) {
  var origin = {};
  var k = 'A'.charCodeAt(0);
  for (var key in schema) {
    var compressedKey = String.fromCharCode(k++);
    if (!goog.isDef(object[compressedKey])) {
      origin[key] = schema[key].def;
    } else {
      var value = this.uncompressValue_(object[compressedKey], schema[key]);
      origin[key] = value;
    }
  }
  return origin;
};

/**
 * The algorithm that transform keys is very simple, the first property will be
 * 'a', then next one wil be 'b',... .
 * @param {String} key Key to compress. Unused for now.
 * @return {String} key Key compresssed.
 * @private
 */
bluemind.storage.Serializer.prototype.compressKey_ = function(key) {
  return String.fromCharCode(this.nextKey_++);
};


/**
 * Transform a value depending on it kind
 * @param {*} value Value to compress.
 * @param {Object} schema This value schema.
 * @return {*} compressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.compressValue_ = function(value, schema) {
  if (goog.isDefAndNotNull(value)) {
    switch (schema.kind) {
      case bluemind.storage.Serializer.Type.UID:
        return this.compressUID_(value);
      case bluemind.storage.Serializer.Type.INTEGER:
        return this.compressInt_(value);
      case bluemind.storage.Serializer.Type.STRING:
        return this.compressString_(value);
      case bluemind.storage.Serializer.Type.ENUM:
        return this.compressEnum_(value, schema.values);
      case bluemind.storage.Serializer.Type.SECONDS:
        return this.compressSeconds_(value);
      case bluemind.storage.Serializer.Type.BOOLEAN:
        return this.compressBoolean_(value);
      case bluemind.storage.Serializer.Type.DATE:
        return this.compressDate_(value);
      case bluemind.storage.Serializer.Type.OBJECT:
        return this.serialize(value, schema.item);
      case bluemind.storage.Serializer.Type.ARRAY:
        return this.compressArray_(value, schema.item);
      default:
        return this.compressString_(value);
    }
  }
  return null;
};


/**
 * Transform a value depending on it kind
 * @param {*} value Value to uncompress.
 * @param {Object} schema Value schema.
 * @return {*} uncompressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.uncompressValue_ =
  function(value, schema) {
  if (goog.isDefAndNotNull(value)) {
    switch (schema.kind) {
      case bluemind.storage.Serializer.Type.UID:
        return this.uncompressUID_(value);
      case bluemind.storage.Serializer.Type.INTEGER:
        return this.uncompressInt_(value);
      case bluemind.storage.Serializer.Type.STRING:
        return this.uncompressString_(value);
      case bluemind.storage.Serializer.Type.ENUM:
        return this.uncompressEnum_(value, schema.values);
      case bluemind.storage.Serializer.Type.SECONDS:
        return this.uncompressSeconds_(value);
      case bluemind.storage.Serializer.Type.BOOLEAN:
        return this.uncompressBoolean_(value);
      case bluemind.storage.Serializer.Type.DATE:
        return this.uncompressDate_(value);
      case bluemind.storage.Serializer.Type.OBJECT:
        return this.unserialize(value, schema.item);
      case bluemind.storage.Serializer.Type.ARRAY:
        return this.uncompressArray_(value, schema.item);
      default:
        return this.uncompressString_(value);
    }
  }
  return null;
};


/**
 * Basic value setter.
 * Value is not compressed
 * @param {String} key Property to store.
 * @param {*} value Value to store.
 * @param {boolean} isDefault True if 'value' is the default value for the key.
 */
bluemind.storage.Serializer.prototype.addValue =
  function(key, value, isDefault) {
  var compressedKey = this.compressKey_(key);
  if (!isDefault) {
    this.object_[compressedKey] = value;
  }
};

/**
 * Value is not compressed but could be by using the local storage to store.
 * @param {string} value Value to compress.
 * @return {string} Compressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.compressUID_ = function(value) {
  return value;
};

/**
 * Nothing to do here
 * @param {number} value Value to compress.
 * @return {number} Compressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.compressInt_ = function(value) {
  return value;
};

/**
 * Nothing to do here
 * @param {string} value Value to compress.
 * @return {string} Compressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.compressString_ = function(value) {
  return value;
};

/**
 * Something might be done to compress value
 * @param {*} value Value to compress.
 * @param {Array.<*>} values Enum possible values;.
 * @return {*} Compressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.compressEnum_ = function(value, values) {
  var v = goog.array.indexOf(values, value);
  return (v >= 0) ? v : value;
};

/**
 * Since time are mostly in minutes the value will be divided by 60.
 * @param {number} value Value to compress.
 * @return {number} Compressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.compressSeconds_ = function(value) {
  return (value / 60);
};

/**
 * Value is transformed into an integer.
 * @param {boolean} value Value to compress.
 * @return {number} Compressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.compressBoolean_ = function(value) {
  return (value ? 1 : 0);
};

/**
 * Date is transformed into minutes since 1970.
 * @param {goog.date.DateLike} value Value to compress.
 * @return {number} Compressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.compressDate_ = function(value) {
  var val = (goog.isDateLike(value)) ? value.getTime() : value;
  return this.compressSeconds_(val / 1000);
};


/**
 * .
 * @param {array} value Value to compress.
 * @param {Object} schema Value schema.
 * @return {array} Compressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.compressArray_ = function(value, schema) {
  if (!schema) {
    return value;
  } else {
    var compressedValue = [];
    for (var i = 0; i < value.length; i++) {
      var v = this.compressValue_(value[i], schema);
      compressedValue.push(v);
    }
    return compressedValue;
  }
};


/**
 * Value is not compressed but could be by using the local storage to store.
 * @param {string} value Value to compress.
 * @return {string} Uncompressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.uncompressUID_ = function(value) {
  return value;
};

/**
 * Nothing to do here
 * @param {number} value Value to compress.
 * @return {number} Uncompressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.uncompressInt_ = function(value) {
  return value;
};

/**
 * Nothing to do here
 * @param {string} value Value to compress.
 * @return {string} Compressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.uncompressString_ = function(value) {
  return value;
};

/**
 * Something might be done to compress value
 * @param {*} value Value to compress.
 * @param {Array.<*>} values Enum possible values;.
 * @return {*} Compressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.uncompressEnum_ =
  function(value, values) {
  return values[parseInt(value)] ? values[parseInt(value)] : value;
};

/**
 * Since time are mostly in minutes the value will be divided by 60.
 * @param {number} value Value to compress.
 * @return {number} Compressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.uncompressSeconds_ = function(value) {
  if (value != null) return value * 60;
  return value;
};

/**
 * Value is transformed into an integer.
 * @param {boolean} value Value to compress.
 * @return {number} Compressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.uncompressBoolean_ = function(value) {
    if (value != null) return (value == 1);
    return value;
};

/**
 * FIXME: Return a date instead of a ts...
 * Date is transformed into minutes since 1970.
 * @param {goog.date.DateLike} value Value to compress.
 * @return {number} Compressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.uncompressDate_ = function(value) {
  var val = this.uncompressSeconds_(value);
  if (val != null) return val * 1000;
  return val;
};

/**
 * .
 * @param {array} value Value to compress.
 * @param {Object} schema Value schema.
 * @return {array} Compressed value.
 * @private
 */
bluemind.storage.Serializer.prototype.uncompressArray_ =
  function(value, schema) {
  if (!schema || !goog.isDefAndNotNull(value)) {
    return value;
  } else {
    var uncompressedValue = [];
    for (var i = 0; i < value.length; i++) {
      var v = this.uncompressValue_(value[i], schema);
      uncompressedValue.push(v);
    }
    return uncompressedValue;
  }
};

/**
 * @type {Enum}
 */
bluemind.storage.Serializer.Type = {
  UID: 1,
  INTEGER: 2,
  STRING: 3,
  ENUM: 4,
  SECONDS: 5,
  BOOLEAN: 6,
  DATE: 7,
  ARRAY: 8,
  OBJECT: 9
};

