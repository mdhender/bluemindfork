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
 */

/**
 * @fileoverview Helper for storage related utilities. FIXME : Another way
 *               should be found for storage accessor creation.
 */

goog.provide('bluemind.storage.StorageHelper');
goog.provide('bluemind.storage.StorageHelper.Kind');

goog.require('bluemind.storage.Storage');
goog.require('bluemind.storage.ObjectStorage');
goog.require('bluemind.storage.mechanism.CacheMechanism');
goog.require('goog.async.Deferred');
goog.require('goog.userAgent');
goog.require('goog.userAgent.product');
goog.require('goog.userAgent.product.isVersion');
goog.require('goog.storage.ExpiringStorage');
goog.require('goog.storage.mechanism.HTML5LocalStorage');
goog.require('goog.storage.mechanism.HTML5SessionStorage');
goog.require('ydn.db.Storage');
goog.require('ydn.db.events.Types');

/**
 * The constructor should never be accessed since this is a collection of static
 * methods.
 * 
 * @constructor
 */
bluemind.storage.StorageHelper = function() {
};

/**
 * @enum {string}
 */
bluemind.storage.StorageHelper.Kind = {
  LOCAL : 'local',
  SESSION : 'session',
  DB : 'db'
};
/**
 * Standard storage whith the default mechanism. FIXME
 * 
 * @type {bluemind.storage.Storage|ydn.db.Storage} Storage accessor.
 */
bluemind.storage.StorageHelper.storage = null;

/**
 * HTML5 web storage whith the default mechanism.
 * 
 * @type {bluemind.storage.Storage} Storage accessor.
 */
bluemind.storage.StorageHelper.webStorage;

/**
 * HTML5 session storage whith the default mechanism.
 * 
 * @type {bluemind.storage.Storage} Storage accessor.
 */
bluemind.storage.StorageHelper.sessionStorage;

/**
 * Expiring storage with HTML5 local storage whith the default mechanism.
 * 
 * @type {goog.storage.ExpiringStorage} Storage accessor.
 */
bluemind.storage.StorageHelper.expiringStorage;

/**
 * Automatically selected storage kind.
 * 
 * @type {bluemind.storage.StorageHelper.Kind} Storage kind.
 */
bluemind.storage.StorageHelper.kind = bluemind.storage.StorageHelper.Kind.DB;

/**
 * Get a standard storage whith de default mechanism. TODO: It can be either the
 * local storage or the session storage or maybe IE Data storage depending on
 * user preferences and maybe user agent
 * 
 * @return {bluemind.storage.Storage | ydn.db.Storage} Storage accessor.
 */
bluemind.storage.StorageHelper.getStorage = function() {
  return bluemind.storage.StorageHelper.storage;
};

/**
 * Get the storage for data with an expiration feature.
 * 
 * @return {goog.storage.ExpiringStorage} Storage accessor.
 */
bluemind.storage.StorageHelper.getExpiringStorage = function() {
  if (!bluemind.storage.StorageHelper.expiringStorage) {
    var mechanism = new goog.storage.mechanism.HTML5LocalStorage();
    bluemind.storage.StorageHelper.expiringStorage = new goog.storage.ExpiringStorage(mechanism);
  }
  return bluemind.storage.StorageHelper.expiringStorage;
};

/**
 * Get the storage for data with an expiration feature.
 * 
 * @return {bluemind.storage.Storage} Storage accessor.
 */
bluemind.storage.StorageHelper.getWebStorage = function() {
  if (!bluemind.storage.StorageHelper.webStorage) {
    var mechanism = new goog.storage.mechanism.HTML5LocalStorage();
    bluemind.storage.StorageHelper.webStorage = new bluemind.storage.Storage(mechanism);
  }
  return bluemind.storage.StorageHelper.webStorage;
};

/**
 * Get the storage for temporary storage (Session Storage).
 * 
 * @return {bluemind.storage.Storage} Storage accessor.
 */
bluemind.storage.StorageHelper.getSessionStorage = function() {
  if (!bluemind.storage.StorageHelper.sessionStorage) {
    var mechanism = new goog.storage.mechanism.HTML5SessionStorage();
    bluemind.storage.StorageHelper.sessionStorage = new bluemind.storage.Storage(mechanism);
  }
  return bluemind.storage.StorageHelper.sessionStorage;
};

/**
 * Clear the entire localstorage. Will pshit all your data.
 */
bluemind.storage.StorageHelper.clear = function() {
  switch (bluemind.storage.StorageHelper.kind) {
  case bluemind.storage.StorageHelper.Kind.DB:
    bluemind.storage.StorageHelper.clearLocalStorage();
    return bluemind.storage.StorageHelper.storage.clear();
  case bluemind.storage.StorageHelper.Kind.SESSION:
    var deferred = new goog.async.Deferred();
    bluemind.storage.StorageHelper.clearSessionStorage();
    deferred.callback();
    return deferred;
  default:
    var deferred = new goog.async.Deferred();
    bluemind.storage.StorageHelper.clearLocalStorage();
    deferred.callback();
    return deferred;
  }
};

/**
 * Clear the entire sessionstorage. Will pshit all your data.
 */
bluemind.storage.StorageHelper.clearSessionStorage = function() {
  var mechanism = new goog.storage.mechanism.HTML5SessionStorage();
  mechanism.clear();
};

/**
 * Clear the entire localstorage. Will pshit all your data.
 */
bluemind.storage.StorageHelper.clearLocalStorage = function() {
  var mechanism = new goog.storage.mechanism.HTML5LocalStorage();
  mechanism.clear();
};

/**
 * Clear App localstorage
 * 
 * @param {string} app app name
 */
bluemind.storage.StorageHelper.clearAppData = function(app) {
  switch (bluemind.storage.StorageHelper.kind) {
  case bluemind.storage.StorageHelper.Kind.DB:
    var mechanism = new goog.storage.mechanism.HTML5LocalStorage();
    bluemind.storage.StorageHelper.clearAppDataStorage_(mechanism, app);
    return bluemind.storage.StorageHelper.storage.clear();
  case bluemind.storage.StorageHelper.Kind.SESSION:
    var deferred = new goog.async.Deferred();
    var mechanism = new goog.storage.mechanism.HTML5SessionStorage();
    bluemind.storage.StorageHelper.clearAppDataStorage_(mechanism, app);
    deferred.callback();
    return deferred;
  default:
    var deferred = new goog.async.Deferred();
    var mechanism = new goog.storage.mechanism.HTML5LocalStorage();
    bluemind.storage.StorageHelper.clearAppDataStorage_(mechanism, app);
    deferred.callback();
    return deferred;
  }

};

/**
 * Clear data from storage
 * 
 * @param storage Storage
 * @param app app name
 */
bluemind.storage.StorageHelper.clearAppDataStorage_ = function(storage, app) {
  if (app == 'calendar') {
    storage.remove('lock-bluemind.calendar.sync.EventService');
    storage.remove('calendar-data');
    storage.remove('calendar-version');
    storage.remove('calendars');
    storage.remove('alert');
    storage.remove('notification');
    storage.remove('auth-token');
    var keys = goog.iter.toArray(storage.__iterator__(true));
    goog.array.forEach(keys, function(key) {
      if (goog.string.startsWith(key, 'calendar-')) {
        storage.remove(key);
      }
    });
  } else if (app == 'contact') {
    var cdata = storage.get('calendar-data');
    var cversion = storage.get('calendar-version');
    var ccalendars = storage.get('calendars');
    var cnotification = storage.get('notification');
    var calert = storage.get('alert');
    var cauthtoken = storage.get('auth-token');
    var calendars = new goog.structs.Map();
    var keys = goog.iter.toArray(storage.__iterator__(true));
    goog.array.forEach(keys, function(key) {
      if (goog.string.startsWith(key, 'calendar-')) {
        calendars.set(key, storage.get(key));
      }
    });

    storage.clear();
    if (cdata) {
      storage.set('calendar-data', cdata);
    }
    if (cversion) {
      storage.set('calendar-version', cversion);
    }
    storage.set('calendars', ccalendars);
    storage.set('alert', calert);
    storage.set('notification', cnotification);
    storage.set('auth-token', cauthtoken);
    goog.array.forEach(calendars.getKeys(), function(k) {
      storage.set(k, calendars.get(k));
    });
  } else {
  }
};
