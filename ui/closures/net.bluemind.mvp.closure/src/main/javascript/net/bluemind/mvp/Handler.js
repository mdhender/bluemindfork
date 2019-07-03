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

/** @fileoverview Application handler */

goog.provide('net.bluemind.mvp.Handler');

goog.require('goog.Disposable');

/**
 * Abstract class for application handlers. Handlers are lightweight interface
 * between the router and the application components. Handlers are created and
 * destroy by the routing process.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {goog.Disposable}
 */
net.bluemind.mvp.Handler = function(ctx) {
  goog.base(this);
};
goog.inherits(net.bluemind.mvp.Handler, goog.Disposable);

/**
 * The setup method is called when a navigation event is mapped to a handler
 * class that is not already active. The handle method is not called when the
 * setup method is called
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @return {?goog.Promise|undefined} eventually return a async execution
 *         sequence.
 */
net.bluemind.mvp.Handler.prototype.setup = goog.abstractMethod;

/**
 * When an instance of this handler class is already handling a request,
 * transition will be called if the new navigation event is mapped to this same
 * class.
 * 
 * @return {?goog.Promise|undefined} eventually return a async execution
 *         sequence.
 */
net.bluemind.mvp.Handler.prototype.handle = goog.abstractMethod;

/**
 * When an instance of this handler class is already handling a request,
 * navigate will be called on any new navigation event.
 * 
 * @param {boolean} exit True if the handler will be disposed in the new
 *          navigation event.
 * @return {?goog.Promise|undefined} eventually return a async execution
 *         sequence.
 */
net.bluemind.mvp.Handler.prototype.onNavigation = goog.abstractMethod;

/**
 * When an instance of this handler class is already handling a request,
 * navigate will be called on any new navigation event.
 * 
 * @return {?goog.Promise|undefined} eventually return a async execution
 *         sequence.
 */
net.bluemind.mvp.Handler.prototype.exit = goog.abstractMethod;

/**
 * When an handler handle a navigation event this method is called when there is
 * an unhandled exception.
 * 
 * @param {*} error Error cause.
 * @return {?goog.Promise|undefined} eventually return a async execution
 *         sequence.
 */
net.bluemind.mvp.Handler.prototype.error = function(error) {
  throw error;
};
