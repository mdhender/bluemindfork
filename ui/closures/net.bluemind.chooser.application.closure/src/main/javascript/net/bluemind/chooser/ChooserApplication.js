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

goog.provide('net.bluemind.chooser.ChooserApplication');

goog.require('goog.events.EventTarget');
goog.require('net.bluemind.chooser.breadcrumb.BreadcrumbHandler');
goog.require('net.bluemind.chooser.fileexplorer.FileExplorerHandler');
goog.require('net.bluemind.chooser.modules.ModulesHandler');
goog.require('net.bluemind.chooser.search.SearchHandler');
goog.require('net.bluemind.chooser.select.SelectHandler');
goog.require('net.bluemind.filehosting.api.FileHostingClient');
goog.require('net.bluemind.mvp.Application');
goog.require('net.bluemind.mvp.logo.LogoHandler');
goog.require('goog.Timer');


/**
 * @constructor
 *
 * @extends {net.bluemind.mvp.Application}
 */
net.bluemind.chooser.ChooserApplication = function() {
  var routes = [{
    path: '.*',
    handlers: [net.bluemind.mvp.logo.LogoHandler, net.bluemind.chooser.search.SearchHandler,
      net.bluemind.chooser.modules.ModulesHandler, net.bluemind.chooser.breadcrumb.BreadcrumbHandler,
      net.bluemind.chooser.fileexplorer.FileExplorerHandler, net.bluemind.chooser.select.SelectHandler]
  }];
  net.bluemind.mvp.Application.call(this, 'chooser', '/chooser/', routes);
};
goog.inherits(net.bluemind.chooser.ChooserApplication, net.bluemind.mvp.Application);


/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.chooser.ChooserApplication.prototype.ctx;


/** @override */
net.bluemind.chooser.ChooserApplication.prototype.registerHandlers = function(ctx) {
  goog.base(this, 'registerHandlers', ctx);
  ctx.handler('selection', new goog.events.EventTarget());
};


/** @override */
net.bluemind.chooser.ChooserApplication.prototype.bootstrap = function(ctx) {
  this.ctx = ctx;
  return goog.base(this, 'bootstrap', ctx).then(function() {
    this.setOptions({})
  }, null, this);
};


/**
 * Options are :
 *
 * <pre>
 * - selfObject (Object) : Bind object for success and cancel action (Default window.opener or window).
 * - success (function) : On select button hit. Argument will be an array of files info (path)
 * - cancel (function) : On cancel button hit. No arguments.
 * - multi : Can select more than one file. Even if false 'success' function argument will be an array. (Default: true);
 * - close : Close window after action. Default is true (if possible)
 * - linkType, filenameFilter: TODO
 * </pre>
 *
 * @export
 * @param {Object.<string, *>} options
 *
 */
net.bluemind.chooser.ChooserApplication.prototype.setOptions = function(options) {
  var selfObj = goog.global;
  if ('selfObject' in options) {
    selfObj = options['selfObject'];
  } else if (goog.global.opener) {
    selfObj = goog.global.opener;
  }
  
  selfObj.bluendmindChooser = selfObj.bluendmindChooser || {};
  
  
  
  var timer = new goog.Timer(3000);
  var trials = 0;
  goog.events.listen(timer, goog.Timer.TICK, function() {
    if (goog.isDefAndNotNull(selfObj) && !selfObj.closed) {
      trials = 0;
    } else if (trials == 3) {
      goog.global.close();
    } else {
      trials++;
    }
  });

  timer.start();
  
  if ('success' in options) {
    var fn = goog.bind(options['success'], selfObj);
    selfObj.bluendmindChooser.onSuccess = fn;
    this.ctx.session.set('onSuccess', fn);
  } else if (goog.isDefAndNotNull(selfObj.bluendmindChooser.onSuccess)) {
    this.ctx.session.set('onSuccess', selfObj.bluendmindChooser.onSuccess);
  }
  
  if ('cancel' in options) {
    var fn = goog.bind(options['cancel'], selfObj);
    selfObj.bluendmindChooser.onCancel = fn;
    this.ctx.session.set('onCancel', fn);
  } else if (goog.isDefAndNotNull(selfObj.bluendmindChooser.onCancel)) {
    this.ctx.session.set('onCancel', selfObj.bluendmindChooser.onCancel);
  }
  
  if ('multi' in options) {
    selfObj.bluendmindChooser.multi = options['multi'];
    this.ctx.session.set('multiSelect', options['multi']);
  } else if (goog.isDefAndNotNull(selfObj.bluendmindChooser.multi)) {
    this.ctx.session.set('multiSelect', selfObj.bluendmindChooser.multi);
  } else {
    this.ctx.session.set('multiSelect', true);
  }
  
  if ('close' in options) {
    selfObj.bluendmindChooser.close = options['close'];
    this.ctx.session.set('closeAfterAction', options['close']);
  } else if (goog.isDefAndNotNull(selfObj.bluendmindChooser.close)) {
    this.ctx.session.set('closeAfterAction', selfObj.bluendmindChooser.close);
  } else {
    this.ctx.session.set('closeAfterAction', true);    
  }
};

