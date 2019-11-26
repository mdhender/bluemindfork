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
 * @fileoverview Interface for contact model management.
 */

goog.provide('net.bluemind.container.persistence.IItemHome');
goog.require('goog.async.Deferred');

/**
 * Interface for the different contact storage backend.
 * 
 * @interface
 */
net.bluemind.container.persistence.IItemHome = function() {
};

/**
 * Get Item detail by id.
 * 
 * @param {string} containerId
 * @param {string} id Item id
 * @return {goog.async.Deferred} Deferred object containing Item .
 */
net.bluemind.container.persistence.IItemHome.prototype.getItem = function(containerId, id) {
};

/**
 * Delete an Item.
 * 
 * @param {string} containerId
 * @param {string} id Item id
 * @return {goog.async.Deferred} Deferred object containing true .
 */
net.bluemind.container.persistence.IItemHome.prototype.deleteItem = function(containerId, id) {
};

/**
 * Store (update or create) an Item
 * 
 * @param {string} containerId
 * @param {Object} item Item to store
 * @return {goog.async.Deferred} Deferred object containing Item.
 */
net.bluemind.container.persistence.IItemHome.prototype.storeItem = function(containerId, item) {
};

/**
 * Get entries index. This is like a thumb-index of the entries in the
 * container. An index template can be passed. If not net.bluemind.i18n.alphabet
 * is used.
 * 
 * @param {string} containerId
 * @param {Array.<string>=} opt_alphabet Optional alphabet.
 * @return {goog.async.Deferred} Deferred object containing entries list.
 */
net.bluemind.container.persistence.IItemHome.prototype.getIndex = function(containerId, opt_alphabet) {
};

/**
 * Count entries index.
 * 
 * @param {string} containerId
 * @return {goog.async.Deferred} Deferred object containing entries list.
 */
net.bluemind.container.persistence.IItemHome.prototype.count = function(containerId) {
};
/**
 * Get all entries from a given container.
 * 
 * @param {string} containerId
 * @param {number=} opt_offset Optional offset number (default 0).
 * @return {goog.async.Deferred} Deferred object containing entries list.
 */
net.bluemind.container.persistence.IItemHome.prototype.getItems = function(containerId, opt_offset) {
};

/**
 * Get entry position.
 * 
 * @param {string} containerId
 * @param {Object} item Item .
 * @return {goog.async.Deferred} Deferred object containing item position in
 *         folder.
 */
net.bluemind.container.persistence.IItemHome.prototype.getPosition = function(containerId, item) {
};

/**
 * Search entries FIXME, we should pass a containerId set to limit search to
 * some container (addressbook/calendar/etc...)
 * 
 * @param {string} query Distribution list search query string
 * @param {boolean=} opt_wildcard Automatically add a wildcard.
 * @param {number=} opt_offset Optional offset number (default 0).
 * @return {goog.async.Deferred} Deferred object containing entries list.
 */
net.bluemind.container.persistence.IItemHome.prototype.searchItems = function(query, opt_wildcard, opt_offset) {
};

/**
 * Get local changes.
 * 
 * @param {string} containerId
 * @returns {goog.async.Deferred}
 */
net.bluemind.container.persistence.IItemHome.prototype.getLocalChangeSet = function(containerId) {
};
/**
 * Synchronize entries data between client and bm-core.
 * 
 * @return {goog.async.Deferred} Deferred object containing contact list.
 */
net.bluemind.container.persistence.IItemHome.prototype.syncItems = function(containerId, changed, deleted, errors,
    version) {
};
