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

/* BM FileServer API */
var { httpRequest } = ChromeUtils.import("resource://gre/modules/Http.jsm");

function BMFileHostingAPI(aBaseUrl, aAuthKey, aDomainUid) {
    this._logger = Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("BMFileHostingAPI: ");
    this._authKey = aAuthKey;
    this._baseUrl = aBaseUrl + "/api";
    this._domainUid = aDomainUid;
}

BMFileHostingAPI.prototype.store = function(aOnLoad, aOnError, aThis, aExtraHeader, path, file) {
    let url = this._baseUrl + "/filehosting/" + this._domainUid + "/" + encodeURIComponent(path);
    return this._execute(aOnLoad, aOnError, aThis, url, "PUT", aExtraHeader, file);
};

BMFileHostingAPI.prototype.share = function(aOnLoad, aOnError, aThis, path, downloadLimit, expirationDate) {
    let url = this._baseUrl + "/filehosting/" + this._domainUid + "/_share";
    url += "?path=" + encodeURIComponent(path);
    url += "&downloadLimit=" + encodeURIComponent(downloadLimit);
    url += "&expirationDate=" + encodeURIComponent(expirationDate);
    return this._execute(aOnLoad, aOnError, aThis, url, "GET", null, null);
};

BMFileHostingAPI.prototype.unShare = function(aOnLoad, aOnError, aThis, uurl) {
    let url = this._baseUrl + "/filehosting/" + this._domainUid + "/" + encodeURIComponent(uurl) + "/unshare";
    return this._execute(aOnLoad, aOnError, aThis, url, "DELETE", null, null);
};

BMFileHostingAPI.prototype.getConfig = function(aOnLoad, aOnError, aThis) {
    let url = this._baseUrl + "/attachment/" + this._domainUid + "/_config";
    return this._execute(aOnLoad, aOnError, aThis, url, "GET", null, null);
};

BMFileHostingAPI.prototype.detach = function(aOnLoad, aOnError, aThis, aExtraHeader, name, file) {
    let url = this._baseUrl + "/attachment/" + this._domainUid + "/" + encodeURIComponent(name) + "/share";
    return this._execute(aOnLoad, aOnError, aThis, url, "PUT", aExtraHeader, file);
};

BMFileHostingAPI.prototype._execute = function(aOnLoad, aOnError, aThis, aUrl, aMethod, aExtraHeader, aData) {
    let hds = [["X-BM-ApiKey", this._authKey]];
    let self = this;
    let options = {
        headers: aExtraHeader ? hds.concat(aExtraHeader) : hds,
        postData: aData,
        method: aMethod,
        onLoad: aOnLoad.bind(aThis),
        onError: aOnError.bind(aThis),
        logger: {
            log  : function(aMsg) { self._logger.info(aMsg); },
            debug: function(aMsg) { self._logger.debug(aMsg); }
        }
    };
    return httpRequest(aUrl, options);
}