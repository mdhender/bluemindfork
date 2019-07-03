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
 * A list with sort capabilities.
 */

goog.provide('bluemind.model.SortedList');

goog.require('bluemind.model.ResultList');
goog.require('goog.array');
goog.require('goog.iter.Iterator');
goog.require('goog.iter.StopIteration');
goog.require('goog.structs.Map');

/**
 *  Result list with sort capabilities
 *
 * @param {Array|Object=} opt_values Initial values to start with. 
 * @constructor
 * @extends {bluemind.model.ResultList}
 */
bluemind.model.SortedList = function(opt_values) {
  this.sort_ = [];
  this.compare_ = goog.bind(this.compare, this);
  goog.base(this, opt_values);
};
goog.inherits(bluemind.model.SortedList, bluemind.model.ResultList);

/**
 * Sort index for list content
 * @type {Array}
 */
bluemind.model.SortedList.prototype.sort_;

/** 
 * Sort function binded to 'this'
 * @type {function(Object, Object): number}
 */
bluemind.model.SortedList.prototype.compare_;

/** 
 * Comparison function by which the array is ordered. 
 * Take 2 arguments to compare, and return a negative number, zero, or a 
 * positive number depending on whether the first argument is less than,
 * equal to, or greater than the second.
 * @param {Object} a First element to compare.
 * @param {Object} b Second element to compare.
 * @return {number} negative number, zero, or a positive number.
 */
bluemind.model.SortedList.prototype.compare = function(a, b) {
  var el1 = this.map.get(a);
  var el2 = this.map.get(b);
  return el1 > el2 ? 1 : el1 < el2 ? -1 : 0;
};

/** @override */
bluemind.model.SortedList.prototype.add = function(element) {
  var key = this.getKey(element);
  if (! this.map.containsKey(key)) {
    var e = new bluemind.events.PropertyChangeEvent('list', null, element);  
    this.map.set(key ,element);
    goog.array.binaryInsert(this.sort_, key, this.compare_);
    this.dispatchEvent(e);
  }  
};

/** @override */
bluemind.model.SortedList.prototype.remove = function(element) {
  var key = this.getKey(element);
  if (this.map.containsKey(key)) {
    var e = new bluemind.events.PropertyChangeEvent('list', element, null);  
    var r = this.map.remove(key);
    //FIXME: The binaryRemove is used to prevent a O(n) runtime (binary remove
    // should be O(log(n)). Ask david why this as been changed.
    //goog.array.binaryRemove(this.sort_, key, this.compare_); 
    goog.array.remove(this.sort_, key);
    this.dispatchEvent(e);
  }
};


/**
 * reset the position of element in the sorted index
 * @param {*} element Element to reindex
 * @param {boolean=} opt_force force event dispatch
 * @protected
 */
bluemind.model.SortedList.prototype.reset = function(element, opt_force) {
  var key = this.getKey(element);  
  var i = goog.array.indexOf(this.sort_, key);
  goog.array.removeAt(this.sort_, i)
  goog.array.binaryInsert(this.sort_, key, this.compare_);
  var j = goog.array.indexOf(this.sort_, key);
  if (i != j || opt_force) {
    var e = new bluemind.events.PropertyChangeEvent('position', i, j);  
    this.dispatchEvent(e);
  }
}

/**
 * Returns the index of the first element of the list with a specified
 * value, or -1 if the element is not present in the array.
 * @param {*} obj The object for which we are searching.
 * @return {number} The index of the first matching array element.
 */
bluemind.model.SortedList.prototype.indexOf = function(element) {
  var key = this.getKey(element);
  return goog.array.indexOf(this.sort_, key); 
}

/** @override */
bluemind.model.SortedList.prototype.clear = function() {
  goog.base(this, 'clear');
  this.sort_ = [];
};

/** @override */
bluemind.model.SortedList.prototype.__iterator__ = function(opt_keys) {
  var i = 0;
  var keys = this.sort_;
  var map = this.map;
  var newIter = new goog.iter.Iterator();
  newIter.next = function() {
    if (i >= keys.length) {
      throw goog.iter.StopIteration;
    }
    var key = keys[i++];
    return map.get(key);
  };
  return newIter;  
};

/**
 * Get item at a given index
 * @param {number} index Index of the item to be retrieved
 * @return {*} Item
 */
bluemind.model.SortedList.prototype.getAt = function(index) {
  return this.map.get(this.sort_[index]);
};

/**
 * Get the next index of an element
 * @param {Object} element
 * @return {number} The element index
 */
bluemind.model.SortedList.prototype.searchIndex = function(element) {
  var key = this.getKey(element);
  this.map.set(key ,element);
  var index = goog.array.binarySearch(this.sort_, key, this.compare_); 
  if (index < 0) {
    this.map.remove(key);
    return -(index + 1)
  } 
  return index;
};

