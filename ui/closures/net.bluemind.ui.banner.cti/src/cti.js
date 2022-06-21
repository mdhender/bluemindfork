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

goog.require('net.bluemind.ui.cti.Dialer');
goog.require('net.bluemin.ui.banner.template');
goog.require('goog.events.EventHandler');
goog.require('net.bluemind.cti.api.ComputerTelephonyIntegrationClient');
goog.require('relief.rpc.RPCService');
goog.require('relief.cache.Cache');

function CTIWidgetCreator() {

  var el = goog.soy.renderAsElement(net.bluemin.ui.banner.template.widget);
  return initializeDialer(el);
}

function initializeDialer(el) {
  var handler = new goog.events.EventHandler();

  handler.listen(el, goog.events.EventType.CLICK, function() {
    dialer.toggleVisibility();
  });

  var dialer = new net.bluemind.ui.cti.Dialer(el);
  dialer.setId('dialer');
  dialer.render();

  var rpc = new relief.rpc.RPCService(new relief.cache.Cache(), new goog.structs.Map({
    'X-BM-ApiKey' : goog.global['bmcSessionInfos']['sid'],
    'Accept' : 'application/json'
  }));
  var client = new net.bluemind.cti.api.ComputerTelephonyIntegrationClient(rpc, '',
      goog.global['bmcSessionInfos']['domain'], goog.global['bmcSessionInfos']['userId']);

  handler.listen(dialer, goog.ui.Component.EventType.ACTION, function(e) {
    client.dial(e.number);
  });

  return el;
}

goog.global['CTIWidgetCreator'] = CTIWidgetCreator;

var BmCTIWidget = {
  "name": "BmCTIWidget",
  "template": '<button type="button" v-if="show" class="btn btn-on-fill-primary"><svg aria-hidden="true" focusable="false" data-prefix="fas" data-icon="phone" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 12 12" class="svg-inline--fa fa-phone fa-w-16 fa-lg"><path fill="currentColor" d="M2.78236 0.500011C2.97533 0.404486 3.20784 0.442544 3.36014 0.594584L5.63035 2.86087C5.82596 3.05614 5.82637 3.37261 5.63128 3.56839L4.21472 4.98993L7.06176 7.83205L8.47646 6.41237C8.57048 6.31802 8.69825 6.26489 8.83157 6.26472C8.96488 6.26454 9.09279 6.31733 9.18706 6.41144L11.4053 8.6258C11.5576 8.77784 11.5957 9.00995 11.5 9.20258C11.0608 10.0867 10.5686 10.7294 9.98281 11.0972C9.37161 11.4809 8.7142 11.5309 8.04505 11.3383C7.39946 11.1525 6.74044 10.7415 6.06486 10.2058C5.38431 9.66607 4.65166 8.97068 3.86053 8.1786C3.06718 7.38662 2.36762 6.65104 1.82234 5.96671C1.28094 5.28722 0.863574 4.62428 0.670568 3.97581C0.470948 3.30511 0.511053 2.64319 0.889715 2.02643C1.25314 1.43449 1.89537 0.9391 2.78236 0.500011ZM2.6078 5.34302C3.11629 5.98119 3.78283 6.68414 4.57048 7.47043C5.35574 8.25664 6.05568 8.91893 6.68913 9.42127C7.32763 9.92761 7.86459 10.2436 8.32314 10.3756C8.75815 10.5009 9.11304 10.4597 9.44853 10.249C9.75948 10.0538 10.0912 9.68924 10.4346 9.07371L8.83316 7.47504L7.41846 8.89472C7.32443 8.98907 7.19666 9.0422 7.06335 9.04238C6.93003 9.04255 6.80212 8.98976 6.70785 8.89565L3.15114 5.34508C2.95553 5.14981 2.95512 4.83335 3.15021 4.63756L4.56677 3.21602L2.91162 1.56373C2.29736 1.90626 1.93663 2.23853 1.74542 2.54998C1.53866 2.88674 1.5006 3.24686 1.63263 3.69046C1.77128 4.15629 2.09538 4.69992 2.6078 5.34302Z" class=""></path></svg></button>',
  "data": function() {
    return {"show": goog.global['bmcSessionInfos']['roles'].split(',').includes('hasCTI')}
  },
  "mounted": function() {
    initializeDialer(this["$el"]);
    
  }
}
goog.global['Vue'] && goog.global['Vue']['component']('BmCTIWidget', BmCTIWidget);
