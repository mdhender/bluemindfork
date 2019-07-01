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
 * @fileoverview
 *
 * Bluemind result list. A linked list with a "total" property containing the
 * length of the result set on the server side.
 * It also fire a property change when a value is added or removed.
 */

goog.provide('bluemind.model.ResultList');

goog.require('bluemind.events.PropertyChangeEvent');
goog.require('goog.events.EventTarget');
goog.require('goog.structs.Collection');
goog.require('goog.structs.Map');


/**
 * Bluemind result list
 *
 * @param {Array|Object=} opt_values Initial values to start with. 
 * @constructor
 * @implements {goog.structs.Collection}
 * @extends {goog.events.EventTarget}
 */
bluemind.model.ResultList = function(opt_values) {
  goog.base(this);
  this.map = new goog.structs.Map();
  if (opt_values) {
    this.addAll(opt_values);
  }
};
goog.inherits(bluemind.model.ResultList, goog.events.EventTarget);

/**
 * Page size
 * @type {number}
 * @const
 */
bluemind.model.ResultList.PAGE = 200;

/**
 * Result map 
 * @type {goog.structs.Map.<string, *>}
 * @protected
 */
bluemind.model.ResultList.prototype.map;

/**
 * Length of the result on server side
 * @type {number}
 * @private
 */
bluemind.model.ResultList.prototype.total_;


/**
 * Property change dispatch lock
 * @type {boolean}
 * @private
 */
bluemind.model.ResultList.prototype.lock_ = false;

/**
 * Prevent PropertyChangeEvent to be dispatched
 */
bluemind.model.ResultList.prototype.lock = function() {
  this.lock_ = true;
};

/**
 * Stop preventing PropertyChangeEvent to be dispatched
 */
bluemind.model.ResultList.prototype.unlock = function() {
  this.lock_ = false;
};

/** @override */
bluemind.model.ResultList.prototype.dispatchEvent = function(e) {
  if (!this.lock_ || !(e instanceof bluemind.events.PropertyChangeEvent)) {
    return goog.base(this, 'dispatchEvent', e);
  }
  return false;
};

/**
 * set total number of result.
 * @param {number} total Total number of result.
 */
bluemind.model.ResultList.prototype.setTotal = function(total) {
  if (this.total_ != total) {
    var e = new bluemind.events.PropertyChangeEvent('total', this.total_, total);  
    this.total_ = total;
    this.dispatchEvent(e);
  }
};

/**
 * Get total number of result.
 * @return {number} Total number of result.
 */
bluemind.model.ResultList.prototype.getTotal = function() {
  return this.total_ ;
};

/**
 * Obtains a unique key for an element of the set.  Primitives will yield the
 * same key if they have the same type and convert to the same string.  
 * @param {*} val Object or primitive value to get a key for.
 * @return {string} A unique key for this value/object.
 * @protected
 */
bluemind.model.ResultList.prototype.getKey = function(val) {
  var type = typeof val;
  if (type == 'object' && val || type == 'function') {
    return 'o' + goog.getUid(/** @type {Object} */ (val));
  } else {
    return type.substr(0, 1) + val;
  }  
};

/** @override */
bluemind.model.ResultList.prototype.getCount = function() {
  return this.map.getCount();
};


/** @override */
bluemind.model.ResultList.prototype.add = function(element) {
  var key = this.getKey(element);
  if (! this.map.containsKey(key)) {
    var e = new bluemind.events.PropertyChangeEvent('list', null, element);  
    this.map.set(key ,element);
    this.dispatchEvent(e);
  }
};

/**
 * Adds all the values in the given collection to this set.
 * @param {Array|Object} col A collection containing the elements to add.
 */
bluemind.model.ResultList.prototype.addAll = function(col) {
  var e = new bluemind.events.PropertyChangeEvent('list', null, col);  
  this.lock();
  var values = goog.structs.getValues(col);
  var l = values.length;
  for (var i = 0; i < l; i++) {
    this.add(values[i]);
  }
  this.unlock();
  this.dispatchEvent(e);  
};


/**
 * Removes all values in the given collection from this set.
 * @param {Array|Object} col A collection containing the elements to remove.
 */
bluemind.model.ResultList.prototype.removeAll = function(col) {
  var e = new bluemind.events.PropertyChangeEvent('list', null, null);  
  this.lock();  
  var values = goog.structs.getValues(col);
  var l = values.length;
  for (var i = 0; i < l; i++) {
    this.remove(values[i]);
  }
  this.unlock();
  this.dispatchEvent(e);    
};


/** @override */
bluemind.model.ResultList.prototype.remove = function(element) {
  if (this.contains(element)) {
    var e = new bluemind.events.PropertyChangeEvent('list', element, null);  
    var r = this.map.remove(this.getKey(element));
    this.dispatchEvent(e);
  }
  return r;
};

/**
 * Removes all elements from this set.
 */
bluemind.model.ResultList.prototype.clear = function() {
  var e = new bluemind.events.PropertyChangeEvent('list', null, null);  
  this.map.clear();
  this.dispatchEvent(e);
};

/**
 * Tests whether this set is empty.
 * @return {boolean} True if there are no elements in this set.
 */
bluemind.model.ResultList.prototype.isEmpty = function() {
  return this.map.isEmpty();
};

/** @override */
bluemind.model.ResultList.prototype.contains = function(element) {
  return this.map.containsKey(this.getKey(element));
};

/**
 * Tests whether this set contains all the values in a given collection.
 * Repeated elements in the collection are ignored, e.g.  (new
 * bluemind.model.ResultList([1, 2])).containsAll([1, 1]) is
 * True.
 * @param {Object} col A collection-like object.
 * @return {boolean} True if the set contains all elements.
 */
bluemind.model.ResultList.prototype.containsAll = function(col) {
  return goog.structs.every(col, this.contains, this);
};

/**
 * Returns an array containing all the elements in this set.
 * @return {!Array} An array containing all the elements in this set.
 */
bluemind.model.ResultList.prototype.getValues = function() {
  return this.map.getValues();
};

/**
 * Returns an iterator that iterates over the elements in this set.
 * @param {boolean=} opt_keys This argument is ignored.
 * @return {!goog.iter.Iterator} An iterator over the elements in this set.
 */
bluemind.model.ResultList.prototype.__iterator__ = function(opt_keys) {
  return this.map.__iterator__(false);
};
