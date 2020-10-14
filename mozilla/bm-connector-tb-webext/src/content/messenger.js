/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

/* Main window overlay */

var { cloudFileAccounts } = ChromeUtils.import("resource:///modules/cloudFileAccounts.jsm");
var { percentEncode } = ChromeUtils.import("resource://gre/modules/Http.jsm");

var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");
var { bmFileProvider } = ChromeUtils.import("chrome://bm/content/modules/bmFileProvider.jsm");
var { bmService, BMSyncObserver } = ChromeUtils.import("chrome://bm/content/modules/bmService.jsm");

var gBMOverlay = {
	_initDone: false,
	_syncObserver : new BMSyncObserver(),
	_logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMOverlay: "),
	_consoleListener: null,
	init: async function() {
		if (!this._initDone) {
        	console.trace("INIT");
			this._initDone = true;
			let consoleService = Components.classes["@mozilla.org/consoleservice;1"].getService(Components.interfaces.nsIConsoleService);
        	this._consoleListener = {
				logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject
									.getLogger("Console: "),
				QueryInterface: function(aIID) {
					if (!aIID.equals(Components.interfaces.nsISupports)
						&& !aIID.equals(Components.interfaces.nsIConsoleListener)) {
						throw Components.results.NS_ERROR_NO_INTERFACE;
					}
					return this;
				},
				observe: function(aMsg) {
					//everything logged in js console is logged in file
					let message = aMsg.message;
					if (!message.indexOf("[JavaScript Warning:") == 0)
						this.logger.info(message);
				}
			}
			consoleService.registerListener(this._consoleListener);
			
			this._installCertificates();
        
			//content policy accept websocket tbird >= esr-45
			if (Components.interfaces.nsIMsgContentPolicy) {
				let policy = Components.classes["@mozilla.org/messenger/content-policy;1"].getService(Components.interfaces.nsIMsgContentPolicy);
				policy.addExposedProtocol("wss");
			}

			await bmService.init();
			this._fixPrefs();
			this._syncObserver.register(window);
			this._registerTabObserver();
			this._initFileProvider();
			let needReset = await this._needReset();
			if (needReset) {
				this._logger.info("Connector updated -> reset");
				bmService.reset();
				bmService.doSync();
				this._reloadBmTabs();
			}
			bmService.enablePeriodicallySync();
		}
	},
	_needReset: async function() {
		let prevVersion = bmUtils.getCharPref("extensions.bm.version", "1.0.0-dev");
		let addon = await AddonManager.getAddonByID("bm-connector-tb@blue-mind.net");
		let currVersion = addon.version;
		this._logger.info("Previous version: " + prevVersion);
		this._logger.info("Current version: " + currVersion);
		bmUtils.setCharPref("extensions.bm.version", currVersion);
		return (Services.vc.compare(currVersion, prevVersion) > 0);
	},
	_installCertificates: function() {
        let cert = "cacert.pem";
        this._addCertificate(cert);
    },
    _addCertificate: function(CertName) {
        try {
            let channel;
            let input;
            try {
                let uri = Services.io.newURI("chrome://bm/content/certs/" + CertName);
				channel = Services.io.newChannelFromURI(uri,
														null, //aLoadingNode
														Services.scriptSecurityManager.getSystemPrincipal(),
														null, //aTriggeringPrincipal
														Components.interfaces.nsILoadInfo.SEC_ALLOW_CROSS_ORIGIN_DATA_IS_NULL,
														Components.interfaces.nsIContentPolicy.TYPE_OTHER);
                input = channel.open();
            } catch (e) {
                //file not found
                Components.utils.reportError(e);
                return;
            }
            let certDB = Components.classes["@mozilla.org/security/x509certdb;1"].getService(Components.interfaces.nsIX509CertDB);
            let scriptableStream = Components.classes["@mozilla.org/scriptableinputstream;1"].createInstance(Components.interfaces.nsIScriptableInputStream);
            scriptableStream.init(input);
            let certfile = scriptableStream.read(input.available());
            scriptableStream.close();
            input.close();
            let beginCert = "-----BEGIN CERTIFICATE-----";
            let endCert = "-----END CERTIFICATE-----";
        
            certfile = certfile.replace(/[\r\n]/g, "");
            let begin = certfile.indexOf(beginCert);
            let end = certfile.indexOf(endCert);
            let cert = certfile.substring(begin + beginCert.length, end);
            
            // load cert in memory to get is fingerPrint
            let daCert = certDB.constructX509FromBase64(cert);
            let fingerPrint = (daCert.sha1Fingerprint != null ? daCert.sha1Fingerprint : daCert.md5Fingerprint);
                    
            let exist = this._getCertDbKeyByFingerPrint(fingerPrint);
            if (!exist) {
				certDB.addCertFromBase64(cert, ",,");
				let newKey = this._getCertDbKeyByFingerPrint(fingerPrint);
				let newCert = certDB.findCertByDBKey(newKey);
				certDB.setCertTrust(newCert, Components.interfaces.nsIX509Cert.CA_CERT,
					 Components.interfaces.nsIX509CertDB.TRUSTED_SSL | Components.interfaces.nsIX509CertDB.TRUSTED_EMAIL);
            }
        } catch (e) {
            Components.utils.reportError(e);
        }
    },
    _getCertDbKeyByFingerPrint: function(aFingerPrint) {
        let certDB = Components.classes["@mozilla.org/security/x509certdb;1"].getService(Components.interfaces.nsIX509CertDB);
        let certs = certDB.getCerts();
        for (let crt of certs) {
            if (crt.sha1Fingerprint == aFingerPrint || crt.md5Fingerprint == aFingerPrint) {
                return crt.dbKey;
            }
        }
        return null;
    },
	openBmApp : function (aApp, aBackground) {
		this.openBmApps([{
			bmApp: aApp,
			openInBackGround: aBackground}
		]);
	},
    openBmApps : function (aApps) {
		this._logger.info("Open BM web apps:" + aApps);
        let user = {};
        let pwd = {};
        let srv = {};
        if (bmUtils.getSettings(user, pwd, srv, true, window)) {
          if (!Services.io.offline) {
            this._auth(srv.value, aApps, user.value, pwd.value, aApps);
          } else {
            this._openWebPages(srv.value, aApps);
          }
        } else {
            this._logger.info("not enouth settings");
        }
	},
	_auth: function(aServer, aApps, aLogin, aPassword) {
		let host = aServer.replace("https://", "");
		let cookies = Services.cookies.getCookiesFromHost(host, {});
		for (let cookie of cookies) {
			Services.cookies.remove(cookie.host, cookie.name, cookie.path, {});
		}
		let url = aServer + "/login/native";
		let self = this;
		let rpcClient = Components.classes["@blue-mind.net/rpcclient;1"].getService().wrappedJSObject;
		let cmd = rpcClient.newCommand(url, "GET");
		cmd.onSuccess = function(xhr) {
			if (xhr.responseText.indexOf("<meta name=\"X-BM-Page\" content=\"maintenance\"/>") != -1) {
			self._logger.info("BM server is in maintenance");
				//FIXME what ?
			} else {
				self._logger.debug("Response headers:" + xhr.getAllResponseHeaders());
				let cookies = Services.cookies.getCookiesFromHost(host, {});
				let hpsSession = null;
				for (let cookie of cookies) {
					if (cookie.name == "HPSSESSION") {
						hpsSession = cookie.value;
					}
				}
				self._login(aServer, aApps, aLogin, aPassword, hpsSession);
			}
		};
		cmd.onFailure = function(xhr) {
			if (!xhr.status && !xhr.statusText) {
				self._openWebPages(aServer, aApps);
			} else {
				let resp = xhr.responseText;
				self._logger.error("Fail to login: " + resp);
			}
		};
		rpcClient.execute(cmd);
	},
	_login: function(aServer, aApps, aLogin, aPassword, aHpsSession) {
		//REST login on /api using auth service :
		// fail if BM is unavailalbe, find and update login right part if missing
		let auth = BMAuthService.login(aServer, aLogin, aPassword);
        let self = this;
        auth.then(function(logged) {
			let user = {};
			let pwd = {};
			let srv = {};
			if (bmUtils.getSettings(user, pwd, srv, false)) {
				self._hpsLogin(srv.value, aApps, user.value, pwd.value, aHpsSession);
			} else {
				self._logger.info("not enouth settings");
			}
        }, function(aRejectReason) {
            self._logger.error(aRejectReason);
            if (aRejectReason instanceof BMError) {
				bmUtils.promptService.alert(null, bmUtils.getLocalizedString("dialogs.title"),
					bmUtils.getLocalizedString(aRejectReason.message));
			}
        }).catch(function(err) {
            self._logger.error(err);
		});
	},
	_hpsLogin: function(aServer, aApps, aLogin, aPassword, aHpsSession) {
		let host = aServer.replace("https://", "");
		let url = aServer + "/login/native";
		let postData = [["login", aLogin],
						["password", aPassword],
						["priv", "priv"],
						["askedUri", aApps[0].bmApp],
						["csrfToken", aHpsSession]];
		let data = {
			server: aServer,
			apps: aApps,
			hpsSession: aHpsSession,
			cookiesHost: host
		};
		let self = this;
		let rpcClient = Components.classes["@blue-mind.net/rpcclient;1"].getService().wrappedJSObject;
		let cmd = rpcClient.newCommand(url, "POST");
		cmd.headers = [["Content-Type", "application/x-www-form-urlencoded; charset=utf-8"]];
		cmd.responseNotJson = true;
		cmd.getData = function() {
			return postData.map(p => p[0] + "=" + percentEncode(p[1])).join("&");
		};
		cmd.onSuccess = function(xhr) {
			self._onAuth(xhr.responseText, xhr, data);
		};
		cmd.onFailure = function(xhr) {
			if (!xhr.status && !xhr.statusText) {
				self._openWebPages(data.server, data.apps);
			} else {
				let resp = xhr.responseText;
				self._logger.error("Fail to login: " + resp);
			}
		};
		rpcClient.execute(cmd);
    },
    _onAuth: function(aResponseText, aRequest, aData) {
		if (aResponseText.indexOf("<meta name=\"X-BM-Page\" content=\"maintenance\"/>") != -1) {
			this._logger.info("BM server is in maintenance");
			//FIXME what ?
			//this._openWebCalendar(aData.server, null, aData.background);
		} else {
			this._logger.debug("Response headers:" + aRequest.getAllResponseHeaders());
			let cookies = Services.cookies.getCookiesFromHost(aData.cookiesHost, {});
			let ssoCookie = null;
			for (let cookie of cookies) {
				if (cookie.name == "BMHPS") {
					ssoCookie = cookie.value;
				}
			}
			this._logger.debug("ssoCookie:" + ssoCookie);
			if (!ssoCookie) {
				if (aRequest.responseURL && aRequest.responseURL.indexOf("login/native?authErrorCode") != -1) {
					this._logger.debug("login failed:" + aRequest.responseURL);
					if (aRequest.responseURL.indexOf("authErrorCode=10") != -1) {
						bmUtils.promptService.alert(null, bmUtils.getLocalizedString("dialogs.title"),
							bmUtils.getLocalizedString("errors.SERVER_ERROR"));
					} else {
						bmUtils.promptService.alert(null, bmUtils.getLocalizedString("dialogs.title"),
							bmUtils.getLocalizedString("errors.credentials"));
						bmUtils.removeCredentialStored(aData.server);
						bmUtils.session.password = null;
					}
				}
			} else {
				this._openWebPages(aData.server, aData.apps);
			}
		}
    },
    _getBmTab: function(aAskedUri) {
		let tabmail = document.getElementById("tabmail");
		let tabMode = tabmail.tabModes["bmTab"];
		if (!tabMode) {
			console.error("no bmTab taModes !");
			return null;
		}
		for (let tab of tabMode.tabs) {
			let bmApp = tab.tabNode.getAttribute("bmApp");
			if (aAskedUri == bmApp) {
				return tab;
			}
		}
		return null;
	},
	_openWebPages: function(aServer, aApps) {
		for (let app of aApps) {
			this._openWebPage(aServer, app.bmApp, app.openInBackGround);
		}
	},
    _openWebPage: function(aServer, aAskedUri, aBackground) {
        let url = aServer + aAskedUri + "/index.html";
		if (bmUtils.getBoolPref("extensions.bm.openInTab", false)) {
			this._clickRegExp = new RegExp("^" + aServer);
			let tabmail = document.getElementById("tabmail");
			let tabBm = this._getBmTab(aAskedUri);
			if (!tabBm) {
				this._logger.debug("OPEN TAB background:" + aBackground);
				let options = {
					contentPage: url,
					clickHandler: "specialTabs.siteClickHandler(event, gBMOverlay._clickRegExp);",
					background: aBackground,
					bmApp: aAskedUri
				};
				if (aAskedUri == "/cal") {
					let self = this;
					options.onLoad = function(event, browser) {
						let win = browser.contentWindow.wrappedJSObject;
						win.net.bluemind.ui.eventdeferredaction.DeferredActionScheduler.setNotificationImpl(function(text) {
							let notif = new Notification(bmUtils.getLocalizedString("notification.title"), {body: text});
							notif.onclick = function() {
								let calTab = self._getBmTab("/cal");
								tabmail.switchToTab(calTab);
							};
						});
					};
				}
				if (aAskedUri == "/settings") {
					this.settingsClickHandlder = function(aEvent) {
						if (!aEvent.isTrusted || aEvent.defaultPrevented || aEvent.button) {
							return true;
						}
						let href = hRefForClickEvent(aEvent, true)[0];
						if (href) {
							let uri = makeURI(href);
							if (uri.spec.endsWith("/settings/index.html")) {
								tabmail.closeTab(tabBm);
							}
						}
						specialTabs.siteClickHandler(aEvent, gBMOverlay._clickRegExp);
					}
					options.clickHandler = "gBMOverlay.settingsClickHandlder(event);"
				}
				tabmail.openTab("bmTab", options);
			} else {
				tabmail.switchToTab(tabBm);
			}
		} else {
			bmUtils.setBoolPref("network.protocol-handler.warn-external.http", false);
			bmUtils.setBoolPref("network.protocol-handler.warn-external.https", false);
			let ioservice = Components.classes["@mozilla.org/network/io-service;1"].getService(Components.interfaces.nsIIOService);
			let uriToOpen = ioservice.newURI(url, null, null);
			let extps = Components.classes["@mozilla.org/uriloader/external-protocol-service;1"].getService(Components.interfaces.nsIExternalProtocolService);
			extps.loadURI(uriToOpen, null);
		}
    },
    _reloadBmTabs: function() {
		if (bmUtils.getBoolPref("extensions.bm.openInTab", false)) {
			let apps = ["/cal", "/task", "/settings"];
			let toReOpen = [];
			apps.forEach(function(app) {
				let tab = this._getBmTab(app);
				if (tab) {
					let openInBackGround = !tab.tabNode.selected;
					this._logger.debug("tab.selected:" + tab.tabNode.selected);
					let tabmail = document.getElementById("tabmail");
					tabmail.closeTab(tab);
					toReOpen.push({
						bmApp: app,
						openInBackGround: openInBackGround
					});
				}
			}, this);
			if (toReOpen.length > 0) {
				this.openBmApps(toReOpen);
			}
		}
    },
    _closeBmTabs: function() {
		if (bmUtils.getBoolPref("extensions.bm.openInTab", false)) {
			let apps = ["/cal", "/task", "/settings"];
			apps.forEach(function(app) {
				let tab = this._getBmTab(app);
				if (tab) {
					let tabmail = document.getElementById("tabmail");
					tabmail.closeTab(tab);
				}
			}, this);
		}
	},
	observe: function(aSubject, aTopic, aData) {
		if (aTopic == "reload-bm-tabs") {
			this._reloadBmTabs();
		} else if (aTopic == "close-bm-tabs") {
			this._closeBmTabs();
		} else if (aTopic == "open-bm-settings") {
			this.openBmApp('/settings', false);
		}
    },
    _registerTabObserver: function() {
		let obs = Components.classes["@mozilla.org/observer-service;1"]
			.getService(Components.interfaces.nsIObserverService);
		obs.addObserver(this, "reload-bm-tabs", false);
		obs.addObserver(this, "close-bm-tabs", false);
		obs.addObserver(this, "open-bm-settings", false);
    },
    _unregisterTabObserver: function() {
		let obs = Components.classes["@mozilla.org/observer-service;1"]
			.getService(Components.interfaces.nsIObserverService);
		obs.removeObserver(this, "reload-bm-tabs");
		obs.removeObserver(this, "close-bm-tabs");
		obs.removeObserver(this, "open-bm-settings");
    },
	_initFileProvider: function() {
		if (bmUtils.getSettings({}, {}, {}, true, window)) {
			let accs = cloudFileAccounts.getAccountsForType("BlueMind");
			let fileProvider;
			if (accs.length == 0) {
				fileProvider = cloudFileAccounts.createAccount("BlueMind", {
					onStartRequest: function(){},
					onStopRequest: function(){}
				});
			} else {
				fileProvider = accs[0];
			}
			try {
				fileProvider.refreshUserInfo(true, {
					onStartRequest: function(){},
					onStopRequest: function(){
						if (!fileProvider.wrappedJSObject.canRemoteAttach()) {
							cloudFileAccounts.removeAccount(fileProvider.accountKey);
						}
					}
				});
			} catch(e) {
				console.error(e);
				this._logger.error("Fail to init File provider: " + e);
				cloudFileAccounts.removeAccount(fileProvider.accountKey);
			}
		}
	},
	_fixPrefs: function() {
		/* Prevent Collected contacts ab to become hidden */
		// still needed for TB 78 ??
		// bmUtils.setIntPref("ldap_2.servers.history.position", 2);
		/* Accept all cookies */
		bmUtils.setIntPref("network.cookie.cookieBehavior", 0);
		/* The cookie's lifetime is supplied by the server */
		bmUtils.setIntPref("network.cookie.lifetimePolicy", 0);
		/* self signed or CA not in thunderbird installer */
		bmUtils.setBoolPref("extensions.install.requireBuiltInCerts", false);
		bmUtils.setBoolPref("extensions.update.requireBuiltInCerts", false);
	},
	doSync: function() {
        bmService.doSync(false, window);
	},
	openPreferences: function() {
		this.prefwindow = window.open("chrome://bm/content/preferences/bmPrefWindow.xhtml", "Connector settings", "chrome,resizable,centerscreen");
		this.prefwindow.focus();
	},
	//called by OverlayManager when extension is disabled
	onremove: function() {
		this._logger.info("Overlay removing");
		this._syncObserver.unregister();
		this._unregisterTabObserver();
		cloudFileAccounts.unregisterProvider("BlueMind");
		window.removeEventListener("DOMOverlayLoaded_bm-connector-tb@blue-mind.net", () => { gBMOverlay.init(); });

		gBMIcsBandal.onUnload();
		let consoleService = Components.classes["@mozilla.org/consoleservice;1"].getService(Components.interfaces.nsIConsoleService);
		consoleService.unregisterListener(this._consoleListener);
	}
}

// custom event, fired by the overlay loader after it has finished loading
document.addEventListener("DOMOverlayLoaded_bm-connector-tb@blue-mind.net", () => { gBMOverlay.init(); }, { once: true });

window.addEventListener("offline", function(e) {
	disableBtnWhenOffline('bm-button-sync', true);
}, false);

window.addEventListener("online", function(e) {
	disableBtnWhenOffline('bm-button-sync', false);
}, false);

function disableBtnWhenOffline(btnId, disable) {
	let btn = document.getElementById(btnId);
	if (btn) {
		btn.setAttribute('disabled', disable? 'true' : 'false');
	}
}