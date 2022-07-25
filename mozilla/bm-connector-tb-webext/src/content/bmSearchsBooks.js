/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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

/*Remote search ab*/

var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");
var { BMAuthService } = ChromeUtils.import("chrome://bm/content/modules/core2/BMAuthService.jsm");

var gBMSearchsBooks = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMSearchsBooks: "),
    init: function() {
        let loader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"].getService(Components.interfaces.mozIJSSubScriptLoader);
        loader.loadSubScript("chrome://bm/content/core2/client/AddressBookClient.js");
    },
    searchBook: async function(containerId, searchString) {
        let matches = [];

        let user = {};
        let pwd = {};
        let srv = {};
        if (!bmUtils.getSettings(user, pwd, srv, true)) return matches;

        try {
            let logged = await BMAuthService.login(srv.value, user.value, pwd.value);
            let book = new AddressBookClient(srv.value, logged.authKey, containerId);
        
            let term = "(" + searchString + "*)";
            let q = "value.kind: 'individual'";
            let limit = 10;
            q += " AND (value.identification.formatedName.value:" + term
            + " OR value.communications.emails.value:" + term + ")"
            + " AND _exists_:value.communications.emails.value";
            let query = {query: q, size: limit, from: 0};
            
            let results = await book.search(query);
            this._logger.debug("results:" + results.total);
            for (let result of results.values) {
                matches.push({
                    DisplayName: result.displayName,
                    PrimaryEmail: result.value.mail
                });
            }
            q = "value.kind: 'group'";
            q += " AND (value.identification.formatedName.value:" + term
            + " OR value.communications.emails.value:" + term + ")"
            + " AND _exists_:value.communications.emails.value";
            query = {query: q, size: limit, from: 0};
            
            results = await book.search(query);
            this._logger.debug("results:" + results.total);
            for (let result of results.values) {
                matches.push({
                    DisplayName: result.displayName,
                    PrimaryEmail: result.value.mail
                });
            }
        } catch(e) {
            console.error(e);
        } finally {
            return matches;
        }
    }
};
