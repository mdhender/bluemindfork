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

goog.provide('net.bluemind.ui.BannerStandalone');
goog.provide('bm');

goog.require('net.bluemind.ui.banner.Banner');
goog.require('bluemind.ui.BannerModel');
goog.require('relief.rpc.RPCService');
goog.require('relief.cache.Cache');
goog.require('net.bluemind.authentication.api.AuthenticationClient');

var model = {
    "entries" : [],
    "selectedEntry" : null,
    "widgets": null,
    "user": null
};


net.bluemind.ui.BannerStandalone.instance_ = null;
var banner = new net.bluemind.ui.banner.Banner();
banner.setModel(model);
banner.render(goog.dom.getElement('banner'));

var rpc = new relief.rpc.RPCService(new relief.cache.Cache(), new goog.structs.Map({
      'X-BM-ApiKey' : goog.global['bmcSessionInfos']['sid']
    }));


var authClient = new net.bluemind.authentication.api.AuthenticationClient(rpc, '');

net.bluemind.ui.BannerStandalone.render = function(bannerEl, opt_selectedApp) {
  banner.dispose();
  var header = goog.dom.getElement('header');

  var hideBandal = goog.string.contains(goog.userAgent.getUserAgentString(), 'Thunderbird')
  || goog.string.contains(goog.userAgent.getUserAgentString(), 'Icedove') 
  || header && header.getAttribute('data-banner') == 'false';
  
  if (!hideBandal) {  

	  authClient.getCurrentUser().then(function(authUser) {
	    banner = new bluemind.ui.Banner();
	    model['user'] = authUser;
	    model['entries'] = bluemind.ui.BannerModel.loadEntries(authUser);
	    model['selectedEntry'] = opt_selectedApp;
	    model['widgets'] = bluemind.ui.BannerModel.loadWidgets(model['user']);
	    banner.setModel(model);
	    banner.render(bannerEl);
	
	  });
  }
};

goog.exportSymbol('net.bluemind.ui.BannerStandalone.render', net.bluemind.ui.BannerStandalone.render);
