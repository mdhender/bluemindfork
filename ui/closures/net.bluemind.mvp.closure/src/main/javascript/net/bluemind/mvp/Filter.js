/*
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
 * @fileoverview A filter is an object that performs filtering tasks on the
 *               request to a path.
 */

goog.provide('net.bluemind.mvp.Filter');

/**
 * Filters perform filtering in the filter method. Filters calls are ordered by
 * priority, if a filter throw an exception the filters with a lesser priority
 * are not called. If a filter redirect, but do not throw an exception, the
 * other filters might be called.
 * <p>
 * Example :
 * <ul>
 * <li> Authentication Filters
 * <li> First launch filter
 * <li> New version filter
 * <li> RBAC filter.
 * </ul>
 * 
 * @constructor
 */
net.bluemind.mvp.Filter = function() {
};

/**
 * Filter priority, the lowest the priority is, the sooner the filter will be
 * executed.
 * <ul>
 * <li> >= 100 : Filters that must be executed after other filters.
 * <li> >= 90 : Filters without any constraints.
 * <li> >= 50 : Filters than cannot rewrite the url. From here the filter can
 * use the URL as a determined resource.
 * </ul>
 * 
 * @type {number}
 */
net.bluemind.mvp.Filter.prototype.priority = 99;

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @return {?goog.Promise|undefined} eventually return a async execution
 *         sequence.
 */
net.bluemind.mvp.Filter.prototype.filter = goog.abstractMethod;

/**
 * Filter comparaison function.
 * 
 * @param {net.bluemind.mvp.Filter} f1 Filter to compare.
 * @param {net.bluemind.mvp.Filter} f2 Filter to compare.
 * @return {number}
 */
net.bluemind.mvp.Filter.cmp = function(f1, f2) {
  if (goog.getUid(f1) == goog.getUid(f2))
    return 0;
  if (f1.priority >= f2.priority)
    return 1;
  return -1
};
