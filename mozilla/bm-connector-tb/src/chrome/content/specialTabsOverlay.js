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

/* BM content tab */

specialTabs.bmTabType = {
    __proto__: contentTabBaseType,
    name: "bmTab",
    perTabPanel: "vbox",
    lastBrowserId: 0,
    get loadingTabString() {
      delete this.loadingTabString;
      return this.loadingTabString = document.getElementById("bundle_messenger")
                                             .getString("loadingTab");
    },

    modes: {
      bmTab: {
        type: "bmTab",
        maxTabs: 10
      }
    },

    /**
     * This is the internal function used by bm tabs to open a new tab. To
     * open a bmTab, use specialTabs.openTab("bmTab", aArgs)
     *
     * @param aArgs The options that content tabs accept.
     * @param aArgs.contentPage A string that holds the URL that is to be opened
     * @param aArgs.clickHandler The click handler for that content tab. See the
     *  "Content Tabs" article on MDC.
     * @param aArgs.onLoad A function that takes an Event and a DOMNode. It is
     *  called when the content page is done loading. The first argument is the
     *  load event, and the second argument is the xul:browser that holds the
     *  contentPage. You can access the inner tab's window object by accessing
     *  the second parameter's contentWindow property.
     */
    openTab: function contentTab_onTabOpened(aTab, aArgs) {
      if (!("contentPage" in aArgs))
        throw("contentPage must be specified");

      // First clone the page and set up the basics.
      let clone = document.getElementById("contentTab").firstChild.cloneNode(true);

      const findbar = document.createElement("findbar");
      // Adding browserid to findbar so that browser property can be set
      // in findbar custom element.
      findbar.setAttribute("browserid", "dummycontentbrowser");
      clone.appendChild(findbar);

      clone.setAttribute("id", "bmTab" + this.lastBrowserId);
      clone.setAttribute("collapsed", false);

      let toolbox = clone.firstChild;
      toolbox.setAttribute("id", "bmTabToolbox" + this.lastBrowserId);
      toolbox.firstChild.setAttribute("id", "bmTabToolbar" + this.lastBrowserId);

      aTab.panel.appendChild(clone);
      aTab.root = clone;

      // Start setting up the browser.
      aTab.browser = aTab.panel.querySelector("browser");
      aTab.toolbar = aTab.panel.querySelector(".bmTabToolbar");
      aTab.security = aTab.panel.querySelector(".contentTabSecurity");
      aTab.urlbar = aTab.panel.querySelector(".contentTabUrlbar");
      let openUriParams = null;
      if (aTab.urlbar) {
        //TB >= 65
        aTab.urlbar.textContent = aArgs.contentPage;
        ExtensionParent.apiManager.emit("extension-browser-inserted", aTab.browser);
        openUriParams = {
          triggeringPrincipal: Services.scriptSecurityManager.getSystemPrincipal(),
        };
      }

      // As we're opening this tab, showTab may not get called, so set
      // the type according to if we're opening in background or not.
      let background = ("background" in aArgs) && aArgs.background;
      aTab.browser.setAttribute("type", background ? "content-targetable" :
                                                     "content-primary");

      aTab.browser.setAttribute("id", "bmTabBrowser" + this.lastBrowserId);

      aTab.clickHandler = "clickHandler" in aArgs && aArgs.clickHandler ?
                          aArgs.clickHandler :
                          "specialTabs.defaultClickHandler(event);";
      aTab.browser.setAttribute("onclick", aTab.clickHandler);

      // Set this attribute so that when favicons fail to load, we remove the
      // image attribute and just show the default tab icon.
      aTab.tabNode.setAttribute("onerror", "this.removeAttribute('image');");

      aTab.browser.addEventListener("DOMLinkAdded", DOMLinkHandler, false);
      // gPluginHandler.addEventListeners(aTab.browser);

      // Now initialise the find bar.
      aTab.findbar = aTab.panel.querySelector("findbar");
      aTab.findbar.setAttribute("browserid",
                                "bmTabBrowser" + this.lastBrowserId);

      // Default to reload being disabled.
      aTab.reloadEnabled = false;

      // Now set up the listeners.
      this._setUpLoadListener(aTab);
      this._setUpTitleListener(aTab);
      this._setUpCloseWindowListener(aTab);

      if ("onLoad" in aArgs) {
        aTab.browser.addEventListener("DOMContentLoaded", function _contentTab_onLoad (event) {
          let win = aTab.browser.contentWindow.wrappedJSObject;
          if (!win.tbirdOnLoadCalled) {
            aArgs.onLoad(event, aTab.browser);
            win.tbirdOnLoadCalled = true;
          }
        }, true);
      }

      // Create a filter and hook it up to our browser
      let filter = Components.classes["@mozilla.org/appshell/component/browser-status-filter;1"]
                             .createInstance(Components.interfaces.nsIWebProgress);
      aTab.filter = filter;
      aTab.browser.webProgress.addProgressListener(filter, Components.interfaces.nsIWebProgress.NOTIFY_ALL);

      // Wire up a progress listener to the filter for this browser
      aTab.progressListener = new tabProgressListener(aTab, false);
      aTab.progressListener.addProgressListener({
        onProgressChange: function() {},
        onProgressChange64: function() {},
        onLocationChange: function bm_onLocationChange(aWebProgress, aRequest,
          aLocationURI, aFlags) {
            gBMOverlay._logger.debug("onLocationChange:" + aLocationURI.spec + ", " + aFlags);
            try {
              // nsIURI.path deprecated in tb >= 57
              let path = aLocationURI.pathQueryRef ? aLocationURI.pathQueryRef : aLocationURI.path;
              gBMOverlay._logger.debug("path:" + path);
              if ((path && (path.indexOf("/login/index.html") == 0)
                || (aLocationURI.spec != "about:blank" && aLocationURI.spec.indexOf(aArgs.contentPage) != 0))) {
                let openInBackGround = !aTab.tabNode.selected;
                let tabmail = document.getElementById("tabmail");
                tabmail.closeTab(aTab);
                bmTabsDelayOpener.open({
                  bmApp: aArgs.bmApp,
                  openInBackGround: true
                });
              }
            } catch(e) {
              gBMOverlay._logger.error("onLocationChange:" + e);
            }
          },
          onStateChange: function() {},
          onStatusChange: function() {},
          onSecurityChange: function() {},
          onRefreshAttempted: function() {},
          onContentBlockingEvent: function() {}
      });

      filter.addProgressListener(aTab.progressListener, Components.interfaces.nsIWebProgress.NOTIFY_ALL);

      if ("onListener" in aArgs)
        aArgs.onListener(aTab.browser, aTab.progressListener);

      // Initialize our unit testing variables.
      aTab.pageLoading = false;
      aTab.pageLoaded = false;

      // Now start loading the content.
      aTab.title = this.loadingTabString;

      aTab.tabNode.setAttribute("bmApp", aArgs.bmApp);
      
      if (openUriParams) {
        //TB >= 65
        aTab.browser.loadURI(aArgs.contentPage, openUriParams);
      } else {
        aTab.browser.loadURI(aArgs.contentPage);
      }

      this.lastBrowserId++;
    },
    tryCloseTab: function onTryCloseTab(aTab) {
      let docShell = aTab.browser.docShell;
      // If we have a docshell, a contentViewer, and it forbids us from closing
      // the tab, then we return false, which means, we can't close the tab. All
      // other cases return true.
      return !(docShell && docShell.contentViewer
        && !docShell.contentViewer.permitUnload());
    },
    persistTab: function onPersistTab(aTab) {
      if (aTab.browser.currentURI.spec == "about:blank")
        return null;

      let onClick = aTab.clickHandler;

      return {
        tabURI: aTab.browser.currentURI.spec,
        clickHandler: onClick ? onClick : null,
        bmApp: aTab.tabNode.getAttribute("bmApp")
      };
    },
    restoreTab: function onRestoreTab(aTabmail, aPersistedState) {
      bmTabsDelayOpener.open({
        bmApp: aPersistedState.bmApp,
        openInBackGround: true
      });
    },
  };

window.addEventListener("load", function(e) {
    let tabmail = document.getElementById('tabmail');
    tabmail.registerTabType(specialTabs.bmTabType);
}, false);

let bmTabsDelayOpener = {
  _apps: [],
  _timer: null,
  open: function(aApp) {
    this._apps.push(aApp);
    if (!this._timer) {
      this._timer = Components.classes["@mozilla.org/timer;1"].createInstance(Components.interfaces.nsITimer);
      this._timer.init(this, 1000, Components.interfaces.nsITimer.TYPE_ONE_SHOT);
    }
  },
  observe: function(aSubject, aTopic) {
    if (aTopic == 'timer-callback') {
      gBMOverlay.openBmApps(this._apps);
      this._apps = [];
      this._timer = null;
    }
  }
}
