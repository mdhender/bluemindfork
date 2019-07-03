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
 * @fileoverview Base model object. Inspired from PlastronJs. Add event firing
 *               on every property initiliazed.
 */

goog.provide('bm.mvp.Model');
goog.provide('bm.mvp.model.Schema');

goog.require('net.bluemind.mvp.UID');
goog.require('goog.object');
goog.require('goog.events.EventTarget');

/**
 * Base model object. Add event firing on property update, serialisation (?).
 * 
 * @param {bm.mvp.model.Schema} schema Object description
 * @param {Object.<string, *>=} opt_values Object properties vaues.
 * @extends {goog.events.EventTarget}
 * @constructor
 */
bm.mvp.Model = function(schema, opt_values) {
  goog.base(this);
  // TODO: Parse schema to implement prototype (date, number, string)...
  this.schema = goog.object.clone(bm.mvp.Model.BASE_MODEL);
  goog.object.extend(this.schema, schema);
  this.values_ = {};
  this.history_ = [];
  if (goog.isObject(opt_values)) {
    this.set(opt_values);
  }
  if (!this.has('uid')) {
    this.set('uid', net.bluemind.mvp.UID.generate());
  }
};
goog.inherits(bm.mvp.Model, goog.events.EventTarget);

/**
 * Base model description
 * 
 * @protected
 * @type {bm.mvp.model.Schema}
 */
bm.mvp.Model.BASE_MODEL = {
  'uid' : {}
};

/**
 * Model description
 * 
 * @protected
 * @type {bm.mvp.model.Schema}
 */
bm.mvp.Model.prototype.schema;

/**
 * Object properties values
 * 
 * @type {Object.<string, *>}
 * @private
 */
bm.mvp.Model.prototype.values_;

/**
 * Object properties value versions history
 * 
 * @type {Array.<Object>}
 * @private
 */
bm.mvp.Model.prototype.history_;

/**
 * returns a clean copy of the object
 * 
 * @return {Object.<string, *>} the model as a json.
 */
bm.mvp.Model.prototype.serialize = function() {
  var json = goog.object.clone(this.values_);
  // FIXME: Have a better serialization. (ala unsafeClone,
  // with if(obj.serialize)...)
  var clean = function(obj) {
    if (!goog.isObject(obj))
      return;
    goog.object.forEach(obj, function(key) {
      if (!goog.isDef(obj[key]))
        delete obj[key];
      else if (goog.isObject(obj[key]))
        clean(obj[key]);
    });
  };

  clean(json);

  return json;
};

/**
 * Reset all properties value
 * 
 * @param {boolean=} opt_silent Fire change event if true.
 */
bm.mvp.Model.prototype.reset = function(opt_silent) {
  this.unset(null, true);
  if (!opt_silent) {
    this.change();
  }
};

/**
 * Test property existance.
 * 
 * @param {string} key to test existence of.
 * @return {boolean} whether key exists.
 */
bm.mvp.Model.prototype.has = function(key) {
  return (goog.object.containsKey(this.values_, key) || goog.object.containsKey(this.schema, key))
      && goog.isDef(this.get(key));
};

/**
 * set either a map of key values or a key value
 * 
 * @param {Object|string} key object of key value pairs to set, or the key.
 * @param {*=} opt_val to use if the key is a string, or if key is an object
 * @param {boolean=} opt_silent true if no change event should be fired.
 * @return {boolean} return if succesful.
 */
bm.mvp.Model.prototype.set = function(key, opt_val, opt_silent) {

  // handle key value as string or object
  var success = false;
  if (goog.isString(key)) {
    var temp = {};
    temp[key] = opt_val;
    key = temp;
  }

  this.history_.push(/** @type {Object} */
  (goog.object.unsafeClone(this.values_)));

  // for each key:value try to set using schema else set directly
  goog.object.forEach(key, function(val, key) {
    try {
      var schema = this.schema[key];
      if (!goog.isDef(schema)) {
        schema = this.schema[key] = {};
        // throw Error(key + ' property does not exist.');
      }
      if (goog.isFunction(schema.set)) {
        val = schema.set.call(this, val, opt_silent);
      }
      if (goog.isDef(val)) {
        this.values_[key] = val;
      } else {
        delete this.values_[key];
      }

      // test for change
      if (!success) {
        success = this.testForChange_(key);
      }
      // catch validation errors
    } catch (err) {
      throw err;
    }
  }, this);

  if (success) {
    if (!opt_silent) {
      this.change();
    }
    return true;
  }
  this.history_.pop();
  return false;
};

/**
 * Test if key has changed
 * 
 * @param {string} key Tested property.
 * @return {boolean} has changed.
 * @private
 */
bm.mvp.Model.prototype.testForChange_ = function(key) {
  var get = this.get(key);
  var prev = this.prev(key);
  var schema = this.schema[key];
  if (goog.isFunction(schema.cmp)) {
    var cmp = schema.cmp;
    if (!cmp(get, prev)) {
      return true;
    }
  } else if (goog.isObject(get) && goog.isObject(prev)) {
    if (goog.isFunction(get.equals)) {
      return !get.equals(prev);
    } else {
      return (goog.global.JSON.stringify(get) === goog.global.JSON.stringify(prev));
    }
  }
  return (get !== prev);
};

/**
 * Get property value
 * 
 * @param {Array|string} key to get value of.
 * @param {*=} opt_default will return if value is undefined.
 * @return {*} the value of the key.
 */
bm.mvp.Model.prototype.get = function(key, opt_default) {
  if (goog.isArray(key)) {
    return goog.array.reduce(key, function(obj, k) {
      obj[k] = this.get(k, opt_default);
      return obj;
    }, {}, this);
  }
  var get = this.get_(/** @type {string} */
  (key));
  return goog.isDef(get) ? get : opt_default;
};

/**
 * Internal property value gtter
 * 
 * @param {string} key to get value of.
 * @param {Object.<string, *>=} opt_context Context to get value from
 * @return {*} the value of the key.
 * @private
 */
bm.mvp.Model.prototype.get_ = function(key, opt_context) {
  var schema = this.schema[key];
  var context = opt_context || this.values_;
  if (!goog.isDef(schema)) {
    schema = this.schema[key] = {};
    // throw new Error(key + ' property does not exist.');
  }
  if (schema.get) {
    return this.schema[key].get.apply(this, goog.array.map(this.schema[key].require || [], function(requireKey) {
      if (requireKey === key) {
        return context[key];
      }
      return this.get(requireKey);
    }, this));
  }
  return context[key];

};

/**
 * Unset keys
 * 
 * @param {Array|string=} opt_key to remove .
 * @param {boolean=} opt_silent true if no change event should be fired.
 * @return {boolean} return if succesful.
 */
bm.mvp.Model.prototype.unset = function(opt_key, opt_silent) {
  if (goog.isString(opt_key)) {
    opt_key = [ opt_key ];
  } else if (!goog.isArray(opt_key)) {
    opt_key = goog.object.getKeys(this.values_);
  }
  var temp = {};
  goog.array.forEach(opt_key, function(k) {
    temp[k] = undefined;
  });
  return this.set(temp, opt_key);

};

/**
 * Force the change event dispatch for the model
 */
bm.mvp.Model.prototype.change = function() {
  this.dispatchEvent(goog.events.EventType.CHANGE);
};

/**
 * returns the previous value of the attribute
 * 
 * @param {string} key to lookup previous value of.
 * @return {*} the previous value.
 */
bm.mvp.Model.prototype.prev = function(key) {
  var ctx = /** @type {Object.<string,*>} */
  (goog.array.peek(this.history_));
  return this.get_(key, ctx);
};

/**
 * reverts an object's values to it's last fetch
 * 
 * @param {boolean=} opt_silent whether to fire change event.
 */
bm.mvp.Model.prototype.revert = function(opt_silent) {
  var old = this.history_.pop();
  this.values_ = /** @type {Object.<string, *>} */
  (goog.object.unsafeClone(old));
  if (!opt_silent) {
    this.dispatchEvent(goog.events.EventType.CHANGE);
  }
};

/** @override */
bm.mvp.Model.prototype.dispose = function() {
  this.dispatchEvent(goog.events.EventType.UNLOAD);
  goog.base(this, 'dispose');
};

/** @typedef {Object.<string, *>} */
bm.mvp.model.Schema;

/**
 * @typedef {{silent: boolean, changes: Object.<string, *>}}
 */
bm.mvp.model.ChangeStack;
