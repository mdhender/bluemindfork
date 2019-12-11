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

/* Main window overlay */
Components.utils.import("resource://gre/modules/AddonManager.jsm");
Components.utils.import("resource://gre/modules/Http.jsm");
try {
	Components.utils.import("resource:///modules/cloudFileAccounts.js");
} catch(e) {
	//TB 68
	Components.utils.import("resource:///modules/cloudFileAccounts.jsm");
}
Components.utils.import("resource://bm/bmUtils.jsm");
Components.utils.import("resource://bm/bmService.jsm");
Components.utils.import("resource://bm/core2/BMAuthService.jsm");

if (!Components.interfaces.nsIMsgCloudFileProvider) {
	Components.utils.import("resource://bm/bmFileProvider.jsm");
}

var gBMOverlay = {
    syncObserver : new BMSyncObserver(),
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMOverlay: "),
	_timer: Components.classes["@mozilla.org/timer;1"].createInstance(Components.interfaces.nsITimer),
    doSync: function() {
        bmService.doSync(false, window);
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
    disableLightning: function() {
        let firstAutoDisable = bmUtils.getBoolPref("extensions.bm.lightning.firstAutoDisable", true);
		this._logger.info("Lightning firstAutoDisable:" + firstAutoDisable);
		let self = this;
        if (firstAutoDisable) {
			let callback = function(aAddon) {
				if (!aAddon) return;
                let disabled = aAddon.appDisabled || aAddon.userDisabled;
                self._logger.info("Lightning version:" + aAddon.version +
                                  ", appDisabled: " + aAddon.appDisabled + ", userDisabled: " + aAddon.userDisabled);
                bmUtils.setBoolPref("extensions.bm.lightning.firstAutoDisable", false);
                if (!disabled) {
					aAddon.disable ? aAddon.disable() : aAddon.userDisabled = true;
					self._logger.info("Lightning disabled by connector");
					self._timer.init(function() {
						let ps = Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
											.getService(Components.interfaces.nsIPromptService);
						let choice = ps.confirm(window, bmUtils.getLocalizedString("dialogs.title"),
												bmUtils.getLocalizedString("lightning.restartAfterDisable1") + "\n" +
												bmUtils.getLocalizedString("lightning.restartAfterDisable2") + "\n\n" +
												bmUtils.getLocalizedString("lightning.restartAfterDisable3") + "\n" +
												bmUtils.getLocalizedString("lightning.restartAfterDisable4"));
						if (choice) {
							Services.startup.quit(Components.interfaces.nsIAppStartup.eRestart
								| Components.interfaces.nsIAppStartup.eForceQuit);
						}
					}, 3000, Components.interfaces.nsITimer.TYPE_ONE_SHOT);
                }
			};
			if (ChromeUtils.generateQI) {
				//TB >= 65
				AddonManager.getAddonByID("{e2fda1a4-762b-4020-b5ad-a41df1933103}").then(callback);
			} else {
				AddonManager.getAddonByID("{e2fda1a4-762b-4020-b5ad-a41df1933103}", callback);
			}
        }
    },
	initFileProvider: function() {
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
			if (Components.interfaces.nsIMsgCloudFileProvider
				 && !cloudFileAccounts._providers.has(fileProvider.type)) {
				// workaround TB < 57 provider not registred correctly by XPCOM category
				cloudFileAccounts._providers.set(fileProvider.type, fileProvider);
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
				this._logger.error("Fail to init File provider: " + e);
				cloudFileAccounts.removeAccount(fileProvider.accountKey);
			}
		}
	},
    _auth: function(aServer, aApps, aLogin, aPassword) {
		let host = aServer.replace("https://", "");
		let cookies = Services.cookies.getCookiesFromHost(host, {});
		while (cookies.hasMoreElements()) {
			let cookie = cookies.getNext().QueryInterface(Ci.nsICookie2);
			Services.cookies.remove(cookie.host, cookie.name, cookie.path, false, {});
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
				while (cookies.hasMoreElements()) {
					let cookie = cookies.getNext().QueryInterface(Ci.nsICookie2);
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
			while (cookies.hasMoreElements()) {
				let cookie = cookies.getNext().QueryInterface(Ci.nsICookie2);
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
						win.net.bluemind.deferredaction.reminder.DeferredActionScheduler.setNotificationImpl(function(text) {
							let notif = new Notification(bmUtils.getLocalizedString("notification.title"), {body: text});
							notif.onclick = function() {
								let calTab = self._getBmTab("/cal");
								tabmail.switchToTab(calTab);
							};
						});
					};
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
			let apps = ["/cal", "/task"];
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
			this.openBmApps(toReOpen);
		}
    },
    _closeBmTabs: function() {
		if (bmUtils.getBoolPref("extensions.bm.openInTab", false)) {
			let apps = ["/cal", "/task"];
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
		}
    },
    registerTabObserver: function() {
		let obs = Components.classes["@mozilla.org/observer-service;1"]
			.getService(Components.interfaces.nsIObserverService);
		obs.addObserver(this, "reload-bm-tabs", false);
		obs.addObserver(this, "close-bm-tabs", false);
    },
    unregisterTabObserver: function() {
		let obs = Components.classes["@mozilla.org/observer-service;1"]
			.getService(Components.interfaces.nsIObserverService);
		obs.removeObserver(this, "reload-bm-tabs");
		obs.removeObserver(this, "close-bm-tabs");
    },
    registerAddonObserver: function() {
        AddonManager.addAddonListener({
            onInstalling: function(addon, needsRestart) {
                if (addon.id  == "bm-connector-tb@blue-mind.net") {
                    gBMOverlay._logger.info("bm-connector-tb@blue-mind.net is installed over existing one -> reset data");
                    bmService.reset();
                }
            },
            onUninstalling: function(addon, needsRestart) {
                if (addon.id  == "bm-connector-tb@blue-mind.net") {
                    gBMOverlay._logger.info("bm-connector-tb@blue-mind.net is uninstalled -> reset data");
                    bmService.reset();
                }
            }
        });
		let self = this;
		AddonManager.addInstallListener({
			onInstallEnded: function(addonInstall, addon) {
				if (addon && addon.id == "bm-connector-tb@blue-mind.net" && addon.pendingOperations) {
                    gBMOverlay._logger.info("bm-connector-tb@blue-mind.net install postponed -> ask for restart");
					self._timer.init(function() {
						let ps = Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
											.getService(Components.interfaces.nsIPromptService);
						let choice = ps.confirm(window, bmUtils.getLocalizedString("dialogs.title"),
												bmUtils.getLocalizedString("updated.restartAfterUpdate1") + "\n\n" +
												bmUtils.getLocalizedString("updated.restartAfterUpdate2") + "\n" +
												bmUtils.getLocalizedString("updated.restartAfterUpdate3"));
						if (choice) {
							Services.startup.quit(Components.interfaces.nsIAppStartup.eRestart
								| Components.interfaces.nsIAppStartup.eForceQuit);
						}
					}, 3000, Components.interfaces.nsITimer.TYPE_ONE_SHOT);
                }
			}
		});
    },
	fixPrefs: function() {
		/* Prevent Collected contacts ab to become hidden */
		bmUtils.setIntPref("ldap_2.servers.history.position", 2);
		/* Accept all cookies */
		bmUtils.setIntPref("network.cookie.cookieBehavior", 0);
		/* The cookie's lifetime is supplied by the server */
		bmUtils.setIntPref("network.cookie.lifetimePolicy", 0);
	}
};

window.addEventListener("activate", function firstActivate(e){
	window.removeEventListener("activate", firstActivate, false);
	installButtons();
	gBMOverlay.fixPrefs();
	gBMOverlay.disableLightning();
	gBMOverlay.initFileProvider();
	gBMOverlay.syncObserver.register(window);
	gBMOverlay.registerTabObserver();
	gBMOverlay.registerAddonObserver();
	disableBtnWhenOffline('bm-button-sync', Services.io.offline);
}, false);

window.addEventListener("unload", function(e){
	gBMOverlay.syncObserver.unregister();
	gBMOverlay.unregisterTabObserver();
}, false);

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

function installButton(toolbarId, id, beforeId) {  
	if (!document.getElementById(id)) {  
		let toolbar = document.getElementById(toolbarId);  
		let before = null;  
		if (beforeId) {  
			let elem = document.getElementById(beforeId);  
			if (elem && elem.parentNode == toolbar)  
				before = elem;  
		}  
		toolbar.insertItem(id, before);  
		toolbar.setAttribute("currentset", toolbar.currentSet);  
		if (document.persist) {
			document.persist(toolbar.id, "currentset");
		} else {
			Services.xulStore.persist(toolbar, "currentset");
		}
	}
}  

function installButtons() {  
	installButton("mail-bar3", "bm-button-open-todolist", "gloda-search");
	installButton("mail-bar3", "bm-button-open-calendar", "bm-button-open-todolist");
	installButton("mail-bar3", "bm-button-sync", "bm-button-open-calendar");
}
