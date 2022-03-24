/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software, you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY, without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
*/

const Cc = Components.classes;
const Ci = Components.interfaces;
const Cu = Components.utils;
const Cr = Components.results;

var { Services } = ChromeUtils.import("resource://gre/modules/Services.jsm");
var { cloudFileAccounts } = ChromeUtils.import("resource:///modules/cloudFileAccounts.jsm");
var { FileUtils } = ChromeUtils.import("resource://gre/modules/FileUtils.jsm");

var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");
var { BMAuthService } = ChromeUtils.import("chrome://bm/content/modules/core2/BMAuthService.jsm");

Cu.importGlobalProperties(["File"]);

let cloudFileProvInterface;
if (Ci.nsIMsgCloudFileProvider) {
    cloudFileProvInterface = Ci.nsIMsgCloudFileProvider;
} else {
    // TB >= 67
    cloudFileProvInterface = cloudFileAccounts.constants;
}

this.EXPORTED_SYMBOLS = ["bmFileProvider"];

function bmFileProvider() {
    let loader = Cc["@mozilla.org/moz/jssubscript-loader;1"].getService(Ci.mozIJSSubScriptLoader);
    loader.loadSubScript("chrome://bm/content/core2/FileHosting.js");
    this.wrappedJSObject = this;
}

bmFileProvider.prototype = {
    /* nsISupports */
    QueryInterface: ChromeUtils.generateQI([Ci.nsIMsgCloudFileProvider]),
    classID: Components.ID("{90286309-f57f-4aa7-813b-32b9508d8392}"),
    // The type is a unique string identifier used by various interface elements
    // for styling. As such, the type should be an alphanumpheric string with
    // no spaces.
    get type() { return "BlueMind"; },
    // Unlike the type, the displayName is purely for rendering the name of
    // a storage service provider.
    get displayName() { return "BlueMind"; },
    // A link to the homepage of the service, if applicable.
    get serviceURL() { return ""; },
    /// Some providers might want to provide an icon for the menu
    get iconClass() { return "chrome://bm/content/skin/BM_Icone01_16.png"; },
    get accountKey() { return this._accountKey; },
    get settingsURL() { return "chrome://bm/content/fileProvider/settings.xhtml"; },
    get managementURL() { return "chrome://bm/content/fileProvider/management.xhtml"; },
    // TB 99+
    get reuseUploads() { return true; },
    get serviceName() { return "BlueMind"; },
    get serviceUrl() { return "https://www.bluemind.net"; },

    _logger: Cc["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("bmFileProvider: "),
    _accountKey: null,
    _prefBranch: null,
    _userInfo: null,
    _authKey: null,
    _uploadingFile: false,
    _uploader: null,
    _uploads: [],
    _uploadInfo: {},
    _remoteInfo: {},
    _urlsForFiles : {},
    _expireForUrls : {},
    _maxFileSize: -1, //in bytes
    
    init: function(aAccountKey) {
        this._accountKey = aAccountKey;
        this._prefBranch = Services.prefs.getBranch("mail.cloud_files.accounts." + aAccountKey + ".");
        this._prefBranch.setCharPref("displayName", "BlueMind");
    },
  
    // for TB >= 67
    get configured() {
        return true;
    },
    
     // for TB >= 68
    getPreviousUploads: function() {
        return [];
    },

    /**
     * upload the file to the cloud provider. The callback's OnStopRequest
     * method will be called when finished, with success or an error code.
     *
     * @param aFile file to upload
     *
     * @throws nsIMsgCloudFileProvider.offlineErr if we are offline.
     */
    uploadFile: async function() {
        let aFile;
        if (arguments.length > 1) {
            // TB 91+
            aFile = arguments[1];
        } else {
            aFile = arguments[0];
        }
        let remoteInfo = this._remoteInfo[aFile.path];
        if (remoteInfo) {
            this._logger.info("do not upload remote file:" + aFile.path);
            throw Cr.NS_ERROR_FAILURE;
        }
        let self = this;
        let wrapper = function(aFile) {
            return new Promise(function(resolve, reject) {
                self._uploadFile(aFile, {
                    onStartRequest: function() {},
                    onStopRequest: function(p, ctx, cr) {
                        if (!Components.isSuccessCode(cr)) {
                            throw cr;
                        }
                        let dlUrl =  self._urlsForFiles[aFile.path];
                        let upload = {
                            url: dlUrl, //Next values are for TB 99+
                            name: aFile.leafName,
                            size: aFile.fileSize,
                            serviceName: self.serviceName,
                            serviceIcon: self.iconClass,
                            serviceUrl: self.serviceURL,
                            downloadExpiryDate: {
                                timestamp: self._expireForUrls[dlUrl]
                            },
                        }
                        resolve(upload);
                    }
                });
            });
        };
        async function asyncUpload(aFile) {
            return await wrapper(aFile);
        }
        return asyncUpload(aFile);
    },
    
    /**
     * BM specific share file on the cloud provider. The callback's OnStopRequest
     * method will be called when finished, with success or an error code.
     *
     * @param aFile file already uploaded
     * @param aCallback callback when finished.
     * @param aSkipUpload BM specific: skip upload and share
     *
     * @throws nsIMsgCloudFileProvider.offlineErr if we are offline.
     */
    shareFile: function(aFile, aCallback) {
        this._uploadFile(aFile, aCallback, true);
    },
    
    _uploadFile: function(aFile, aCallback, aSkipUpload) {
        if (Services.io.offline)
            throw cloudFileProvInterface.offlineErr;
        this._logger.info("Upload file: " + aFile.leafName + " skip upload: " + aSkipUpload);
        if (this._uploadingFile && this._uploadingFile != aFile) {
            this._logger.info("Adding file to queue");
            let uploader = new bmFileUploader(this, aFile, this._uploaderCallback.bind(this), aCallback, aSkipUpload);
            this._uploads.push(uploader);
            return;
        }
        this._uploadingFile = aFile;
        
        let onSuccess = function() {
            this._finishUpload(aFile, aCallback, aSkipUpload);
        }.bind(this);
        let onFailure = function() {
            aCallback.onStopRequest(null, null, cloudFileProvInterface.authErr);
        }.bind(this);
    
        return this._login(onSuccess, onFailure, true);
    },
    
    urlForFile: function(aFile) {
        return this._urlsForFiles[aFile.path];
    },
    
    expireForLink: function(aUrl) {
        let expire = this._expireForUrls[aUrl];
        if (expire) {
            return expire;
        }
        return null;
    },
    
    /**
     * Cancels the upload of the passed in file, if it hasn't finished yet.
     * If it hasn't started yet, it will be removed from the upload queue.
     *
     * @param aFile file whose upload we should cancel.
     */
    cancelFileUpload: function(aFile) {
        if (this._uploadingFile.equals(aFile)) {
            this._uploader.cancel();
        } else {
            for (let i = 0; i < this._uploads.length; i++) {
                if (this._uploads[i].file.equals(aFile)) {
                    this._uploads[i].requestObserver.onStopRequest(null, null, cloudFileProvInterface.uploadCanceled);
                    this._uploads.splice(i, 1);
                    return;
                }
            }
        }
    },
  
    /**
     * Refresh the user info for this account. This will fill in the quota info,
     * if supported by the provider.
     *
     * @param aWithUI if true, we may prompt for a password, or bring up an auth
     *                page. If false, we won't, and will fail if auth is required.
     * @param aCallback callback when finished.
     * @throws nsIMsgCloudFileProvider.offlineErr if we are offline.
     */
    refreshUserInfo: function(aWithUI, aCallback) {
        if (Services.io.offline)
            throw cloudFileProvInterface.offlineErr;
        aCallback.onStartRequest(null, null);
        
        let onSuccess = function() {
            aCallback.onStopRequest(null, null, Cr.NS_OK);
        }.bind(this);
        let onFailure = function() {
            aCallback.onStopRequest(null, null, cloudFileProvInterface.authErr);
        }.bind(this);
        
        if (!this._authkey)
            return this._login(onSuccess, onFailure, aWithUI);
        if (!this._userInfo)
            return this._getUserInfo(onSuccess, onFailure);
        return this._userInfo;
    },
  
    /**
     * Delete a file that we've uploaded in this session and discarded. This
     * operation is asynchronous.
     *
     * @param aFile File we previously uploaded in this session.
     * @param aCallback callback when finished
     *
     * @throws NS_ERROR_FAILURE if we don't know about the file.
     * @throws nsIMsgCloudFileProvider.offlineErr if we are offline.
     */
    deleteFile: function(aFile, aCallback) {
        if (Services.io.offline)
            throw cloudFileProvInterface.offlineErr;
        let uploadInfo = this._uploadInfo[aFile.path];
        let remoteInfo = this._remoteInfo[aFile.path];
        if (!uploadInfo && !remoteInfo)
            throw Cr.NS_ERROR_FAILURE;
        
        if (remoteInfo) {
            this._logger.info("do not remove remote file:" + aFile.path);
            if (aCallback) {
                return aCallback.onStopRequest(null, null, Cr.NS_OK);
            } else {
                return Cr.NS_OK
            }
        }
        
        if (!aCallback) {
            let wrapper = function(aFile, aPublicUrl) {
                return new Promise(function(resolve, reject) {
                    this._deleteFile(aFile, aPublicUrl, {
                        onStartRequest: function() {},
                        onStopRequest: function(p, ctx, cr) {
                            if (!Components.isSuccessCode(cr)) {
                                throw cr;
                            }
                            resolve();
                        }
                    });
                }.bind(this));
            }.bind(this);
            async function asyncDelete(aFile, aPublicUrl) {
                return await wrapper(aFile, aPublicUrl);
            }
            return asyncDelete(aFile, uploadInfo.publicUrl);
        } else {
            this._deleteFile(aFile, uploadInfo.publicUrl, aCallback);
        }
    },

    _deleteFile: function(aFile, aPublicUrl, aCallback) {
        let onSuccess = function(aResponseText, aRequest) {
            this._logger.info("success deleting file: " + aResponseText);
            
            if (this._urlsForFiles[aFile.path])
                delete this._urlsForFiles[aFile.path];
            if (this._expireForUrls[aPublicUrl]) {
                delete this._expireForUrls[aPublicUrl];
            }
            delete this._uploadInfo[aFile.path];
            aCallback.onStopRequest(null, null, Cr.NS_OK);
        }.bind(this);
        
        let onFailure = function(aException, aResponseText, aRequest) {
            this._logger.error("fail deleting file: " + aResponseText);
            aCallback.onStopRequest(null, null, cloudFileProvInterface.uploadErr);
        }.bind(this);
        
        this._fileHostingAPI.unShare(
            onSuccess,
            onFailure,
            this,
            aPublicUrl
        );
    },
  
    /**
     * If the provider has an API for creating an account, this will start the
     * process of creating one. There will probably have to be some sort of
     * validation on the part of the user before the account is created.
     * If not, this will throw NS_ERROR_NOT_IMPLEMENTED.
     *
     * If the REST call succeeds, aCallback.onStopRequest will get called back
     * with an http status. Generally, status between 200 and 300 is OK,
     * otherwise, an error occurred which is * probably specific to the provider.
     * If the request fails completely, onStopRequest will get called with
     * Components.results.NS_ERROR_FAILURE
     * @throws nsIMsgCloudFileProvider.offlineErr if we are offline.
     */
    createNewAccount: function(aEmailAddress, aPassword, aFirstName, aLastName, aCallback) {
        return Cr.NS_ERROR_NOT_IMPLEMENTED;
    },
  
    createExistingAccount: function(aCallback) {
        if (Services.io.offline)
            throw cloudFileProvInterface.offlineErr;
        aCallback.onStartRequest(null, null);
        
        let onSuccess = function() {
            aCallback.onStopRequest(null, this, Cr.NS_OK);
        }.bind(this);
        let onFailure = function() {
            aCallback.onStopRequest(null, null, cloudFileProvInterface.authErr);
        }.bind(this);
        
        this._login(onSuccess, onFailure, true);
    },
  
    /**
     * If the provider doesn't have an API for creating an account, perhaps
     * there's a url we can load in a content tab that will allow the user
     * to create an account.
     */
    get createNewAccountUrl() { return ""; },
  
    /**
     * For some errors, the provider may have an explanatory page, or have an
     * option to upgrade an account to handle the error. This method returns a url
     * to a page hosted by the provider which can help with the error.
     *
     * @param aError e.g. uploadWouldExceedQuota or uploadExceedsFileLimit
     * @returns provider url, if any, for the error, empty string otherwise.
     */
    providerUrlForError: function(aError) {
        return Cr.NS_ERROR_NOT_IMPLEMENTED;
    },
  
    /**
     * If we don't know the limit, this will return -1.
     */
    get fileUploadSizeLimit() { return this._maxFileSize; },
  
    /// -1 if we don't have this info
    get remainingFileSpace() { return -1; },
  
    /// -1 if we don't have this info
    get fileSpaceUsed() { return -1; },
  
    /// This is used by our test harness to override the urls the provider uses.
    //overrideUrls: function(aNumUrls, aUrls),
  
    // Error handling
    // If the cloud provider gets textual errors back from the server,
    // they can be retrieved here.
    get lastError() { return ""; },
    
    
    canRemoteAttach: function() {
        let can = this._canRemoteAttach;
        this._logger.info("canRemoteAttach: " + can);
        return can;
    },
    
    canUseFilehosting: function() {
        let can = this._canUseFilehosting;
        this._logger.info("canUseFilehosting: " + can);
        return can;
    },

    // TB 99+
    markAsImmutable: function(id) {
    },
    
    /** Private methods **/
    _login: function(onSuccess, onFailure, aWithUI) {
        let user = {};
        let pwd = {};
        let srv = {};
        if (bmUtils.getSettings(user, pwd, srv, aWithUI)) {
            let self = this;
            let result = BMAuthService.login(srv.value, user.value, pwd.value);
            result.then(function(logged) {
                self._authKey = logged.authKey;
                self._logger.debug("LOGGED with authKey:" + logged.authKey);
                self._loggedUser = logged.authUser;
                self._fileHostingAPI = new BMFileHostingAPI(srv.value, self._authKey, self._loggedUser.domainUid);
                self._canRemoteAttach = self._loggedUser.roles.indexOf("canRemoteAttach") != -1;
                self._canUseFilehosting = self._loggedUser.roles.indexOf("canUseFilehosting") != -1;
                if (self._canRemoteAttach || self._canRemoteAttach) {
                    self._getUserInfo(onSuccess, onFailure);
                } else {
                    onSuccess();
                }
            },
            function(aRejectReason) {
                self._logger.error(aRejectReason);
                onFailure();
            }).catch(function(err) {
                self._logger.error(err);
                onFailure();
            });
        } else {
            onFailure();
        }
    },
    _getUserInfo: function(onSuccess, onFailure) {
        let onOk = function(aResponseText, aRequest) {
            this._logger.info("success get config: " + aResponseText);
            let config = JSON.parse(aResponseText);
            if (config) {
                if (config.autoDetachmentLimit != 0) {
                    // big_attachments.threshold_kb pref is in KiB
                    Services.prefs.getBranch("mail.compose.")
                            .setIntPref("big_attachments.threshold_kb", config.autoDetachmentLimit / 1024);
                }
                this._maxFileSize = (config.maxFilesize == 0 ? -1 : config.maxFilesize);
                this._retentionTime = config.retentionTime;
            }
            onSuccess();
        }.bind(this);
        
        let onError = function(aException, aResponseText, aRequest) {
            this._logger.error("fail to get config: " + aResponseText);
            onFailure();
        }.bind(this);
        
        this._fileHostingAPI.getConfig(onOk, onError, this);
    },
    _finishUpload: function(aFile, aCallback, aSkipUpload) {
        let exceedsFileLimit = cloudFileProvInterface.uploadExceedsFileLimit;
        let exceedsQuota = cloudFileProvInterface.uploadWouldExceedQuota;
        if (this._maxFileSize != -1 && aFile.fileSize > this._maxFileSize)
          return aCallback.onStopRequest(null, null, exceedsFileLimit);
        //if (aFile.fileSize > this.remainingFileSpace)
        //  return aCallback.onStopRequest(null, null, exceedsQuota);
    
        delete this._userInfo;
        if (!this._uploader) {
          this._uploader = new bmFileUploader(this, aFile, this._uploaderCallback.bind(this), aCallback, aSkipUpload);
          this._uploads.unshift(this._uploader);
        }
        this._uploadingFile = aFile;
        this._uploader.uploadFile();
    },
    _uploaderCallback: function(aRequestObserver, aStatus) {
        aRequestObserver.onStopRequest(null, null, aStatus);
        this._uploadingFile = null;
        this._uploads.shift();
        if (this._uploads.length > 0) {
            let nextUpload = this._uploads[0];
            this._logger.info("add new upload file: " + nextUpload.file.leafName);
            this._uploadingFile = nextUpload.file;
            this._uploader = nextUpload;
            try {
                this.uploadFile(nextUpload.file, nextUpload.callback);
            } catch (ex) {
                nextUpload.callback(nextUpload.requestObserver, Cr.NS_ERROR_FAILURE);
            }
        } else {
            this._uploader = null;
        }
    },
};

function bmFileUploader(provider, aFile, aCallback, aRequestObserver, aSkipUpload) {
    this._provider = provider;
    this._logger = provider._logger;
    this._skipUpload = aSkipUpload;
    this.file = aFile;
    this.callback = aCallback;
    this.requestObserver = aRequestObserver;
}

bmFileUploader.prototype = {
    _provider : null,
    file : null,
    callback : null,
    request : null,
    uploadFile: function() {
        this._logger.info("ready to upload file [" + this.file.leafName + "]");
        
        let onSuccess = function(aResponseText, aRequest) {
            this.request = null;
            let detachInfo = JSON.parse(aResponseText);
            this._logger.info("success detach file: " + aResponseText);
            this._provider._uploadInfo[this.file.path] = detachInfo;
            this._provider._urlsForFiles[this.file.path] = detachInfo.publicUrl;
            this._provider._expireForUrls[detachInfo.publicUrl] = detachInfo.expirationDate;
            this.callback(this.requestObserver, Cr.NS_OK);
        }.bind(this);
        
        let onFailure = function(aException, aResponseText, aRequest) {
            this._logger.error("fail detach file: " + aResponseText);
            this.request = null;
            if (this.callback)
                this.callback(this.requestObserver, cloudFileProvInterface.uploadErr);
        }.bind(this);
        
        if (!this._skipUpload) {
            let upload = function(aFile) {
                this.request = this._provider._fileHostingAPI.detach(
                    onSuccess,
                    onFailure,
                    this,
                    null,
                    this.file.leafName,
                    aFile
                );
            }.bind(this);
            File.createFromNsIFile(this.file).then(file => {
                upload(file);
            });
        } else {
           this._logger.info("skip upload file is already on server");
           this._getShareUrl();
        }
        
    },
    cancel: function() {
        this.callback(this.requestObserver, cloudFileProvInterface.uploadCanceled);
        if (this.request) {
            let req = this.request;
            if (req.channel) {
                this._logger.info("cancel channel upload");
                delete this.callback;
                req.channel.cancel(Cr.NS_BINDING_ABORTED);
            }
            this.request = null;
        }
    },
    _getShareUrl: function() {
        let onSuccess = function(aResponseText, aRequest) {
            let shareInfo = JSON.parse(aResponseText);
            this._logger.info("success get share url: " + shareInfo.url);
            this._provider._remoteInfo[this.file.path] = shareInfo;
            this._provider._urlsForFiles[this.file.path] = shareInfo.url;
            this._provider._expireForUrls[shareInfo.url] = shareInfo.expirationDate;
            this.callback(this.requestObserver, Cr.NS_OK);
        }.bind(this);
        
        let onFailure = function(aException, aResponseText, aRequest) {
            this._logger.error("fail to get share url");
            this.callback(this.requestObserver, Cr.NS_ERROR_FAILURE);
        }.bind(this);
        
        //Remove Tmp path from path
        let tmpPath = FileUtils.getDir("TmpD", []).path;
        let filePath = this.file.path.replace(tmpPath, "");
        filePath = filePath.replace(/\\/g, "/");
        
        let expire = "";
        if (this._retentionTime && this._retentionTime > 0) {
            let now = new Date();
            now.setDate(now.getDate() + this._retentionTime);
            expire = "" + now.getTime();
        }
        this._provider._fileHostingAPI.share(
            onSuccess,
            onFailure,
            this,
            filePath,
            0,
            expire
        );
    },
};

// Only one BM account (used only with TB >= 67)
let instanceCount = 0;

if (!Ci.nsIMsgCloudFileProvider) {
    // TB >= 67
    cloudFileAccounts.registerProvider("BlueMind", {
        type: "BlueMind",
        displayName: "BlueMind",
        iconURL: "chrome://bm/content/skin/BM_Icone01_16.png",
        initAccount: function(accountKey) {
            if (instanceCount > 0) {
                return null;
            }
            let account = new bmFileProvider();
            account.init(accountKey);
            instanceCount++;
            return account;
        },
    });
}
