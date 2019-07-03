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
 
// Copyright 2011 The Closure Library Authors. All Rights Reserved.
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
 * @fileoverview Provides a convenient API for data persistence using a selected
 * data storage mechanism.
 *
 */

goog.provide('bluemind.storage.Storage');

goog.require('goog.storage.Storage');
goog.require('goog.storage.mechanism.Mechanism');


/**
 * The bluemind implementation for all storage APIs.
 *
 * @param {!goog.storage.mechanism.Mechanism} mechanism The underlying
 *     storage mechanism.
 * @constructor
 * @extends {goog.storage.Storage} 
 */
bluemind.storage.Storage = function(mechanism) {
  goog.base(this, mechanism);
};
goog.inherits(bluemind.storage.Storage, goog.storage.Storage);


/** @override */
bluemind.storage.Storage.prototype.set = function(key, value) {
  if (!goog.isDef(value)) {
    this.mechanism.remove(key);
    return;
  }
  this.mechanism.set(key, goog.global.JSON.stringify(value));
};

/** @override */
bluemind.storage.Storage.prototype.get = function(key) {
  var json = this.mechanism.get(key);
  if (goog.isNull(json)) {
    return undefined;
  }
  /** @preserveTry */
  try {
    return goog.global.JSON.parse(json);
  } catch (e) {
    throw goog.storage.ErrorCode.INVALID_VALUE;
  }
};
