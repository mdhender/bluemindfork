/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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

//goog.provide("net.bluemind.ui.banner.logout.BannerLogout");

goog.require("goog.Timer");

goog.global['bundleResolve']('net.bluemind.restclient.closure', function() {
    var timer = new goog.Timer(10000);

    timer.addEventListener(goog.Timer.TICK, function() {
        goog.global['restClient'].sendMessage({
            "method" : "GET",
            "path" : "/api/auth/ping",
            "headers" : {
                "X-BM-ApiKey" : goog.global['bmcSessionInfos']['sid']
            },
            "params" : {}
        }, function(res) {
            if (res["statusCode"] == 401) {
                var redirectURL = goog.global['location']['protocol'] + "//" + goog.global['location']['host'];
                redirectURL += goog.global['location']['pathname'].substring(0, window.location.pathname.indexOf('/', 1) + 1);
                goog.global['location'].assign(redirectURL);
            }
        });
    });

    timer.start(); 
});