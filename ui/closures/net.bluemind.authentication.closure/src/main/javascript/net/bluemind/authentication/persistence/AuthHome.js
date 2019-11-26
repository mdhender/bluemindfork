/* BEGIN LICENSE
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

goog.provide("net.bluemind.authentication.persistence.AuthHome");

goog.require("goog.Promise");

/**
 * Interface with the browser storage database to fetch user's data like
 * settings, user login, profiles, roles, etc...
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @suppress {missingProperties}
 */
net.bluemind.authentication.persistence.AuthHome = function(ctx) {
  this.storage_ = ctx.service('database').getDatabase('context');
};

/**
 * Is data stored locally
 * @returns {Boolean}
 */
net.bluemind.authentication.persistence.AuthHome.prototype.isLocal = function() {
  return true;
}

/**
 * @type {ydn.db.Storage}
 */
net.bluemind.authentication.persistence.AuthHome.prototype.storage_;

/**
 * Clear database
 * 
 * @return {goog.Promise} promise with the result
 */
net.bluemind.authentication.persistence.AuthHome.prototype.clear = function() {
  return goog.Promise.resolve().then(function() {
    return this.storage_.clear();
  }, null, this);
};

/**
 * Return a settings from it's namespace and property name
 * 
 * @param {string} property Property name or long name (namespace.property)
 * @param {string=} namespace Property namespace
 * 
 * @return {goog.Promise} promise with the result
 */
net.bluemind.authentication.persistence.AuthHome.prototype.get = function(property, namespace) {
  if (!namespace && property.indexOf('.') > 0) {
    var parts = property.split('.', 2);
    namespace = parts[0];
    property = parts[1];
  }
  namespace = namespace || 'global';
  return this.storage_.get(namespace, property).then(function(value) {
    if (!goog.isDefAndNotNull(value)) {
      var e = new Error('No value found for ' + namespace + '.' + property)
      e.name = 'NotFoundError';
      throw e;
    }
    return value['value'];

  });
};

/**
 * Store a property
 * 
 * Return a settings from it's namespace and property name
 * 
 * @param {string} property Property name or long name (namespace.property)
 * @param {*} value Property value
 * @param {string=} namespace Property namespace
 * @return {goog.Promise} promise with the result
 */
net.bluemind.authentication.persistence.AuthHome.prototype.set = function(property, value, namespace) {
  if (!namespace && property.indexOf('.') > 0) {
    var parts = property.split('.', 2);
    namespace = parts[0];
    property = parts[1];
  }
  namespace = namespace || 'global';
  value = {
    'property' : property,
    'value' : value
  };

  return this.storage_.put(namespace, value, property).then(function(key) {
    return value['value'];
  });
};
