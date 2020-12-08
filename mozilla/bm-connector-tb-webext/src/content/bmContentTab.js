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

specialTabs.bmTabType = {
    __proto__: contentTabBaseType,
    name: "bmTab",
    perTabPanel: "vbox",
    lastBmId: 0,
    get loadingTabString() {
      delete this.loadingTabString;
      return (this.loadingTabString = document
        .getElementById("bundle_messenger")
        .getString("loadingTab"));
    },

    modes: {
      bmTab: {
        type: "bmTab",
        maxTabs: 10,
      },
    },

    /**
     * This is the internal function used by content tabs to open a new tab. To
     * open a contentTab, use specialTabs.openTab("contentTab", aArgs)
     *
     * @param aArgs The options that content tabs accept.
     * @param aArgs.contentPage A string that holds the URL that is to be opened
     * @param aArgs.openWindowInfo The opener window
     * @param aArgs.clickHandler The click handler for that content tab. See the
     *  "Content Tabs" article on MDC.
     * @param aArgs.onLoad A function that takes an Event and a DOMNode. It is
     *  called when the content page is done loading. The first argument is the
     *  load event, and the second argument is the xul:browser that holds the
     *  contentPage. You can access the inner tab's window object by accessing
     *  the second parameter's contentWindow property.
     */
    openTab(aTab, aArgs) {
      console.trace("OPEN TAB:", aTab, aArgs);
      if (!("contentPage" in aArgs)) {
        throw new Error("contentPage must be specified");
      }

      // First clone the page and set up the basics.
      let clone = document
        .getElementById("contentTab")
        .firstElementChild.cloneNode(true);

      clone.setAttribute("id", "bmTab" + this.lastBmId);
      clone.setAttribute("collapsed", false);

      let toolbox = clone.firstElementChild;
      toolbox.setAttribute("id", "bmTabToolbox" + this.lastBmId);
      toolbox.firstElementChild.setAttribute(
        "id",
        "bmTabToolbar" + this.lastBmId
      );

      aTab.linkedBrowser = aTab.browser = document.createXULElement("browser");
      aTab.browser.setAttribute("id", "bmTabBrowser" + this.lastBmId);
      aTab.browser.setAttribute("type", "content");
      aTab.browser.setAttribute("flex", "1");
      aTab.browser.setAttribute("autocompletepopup", "PopupAutoComplete");
      aTab.browser.setAttribute("datetimepicker", "DateTimePickerPanel");
      aTab.browser.setAttribute("context", "mailContext");
      aTab.browser.setAttribute("messagemanagergroup", "browsers");
      aTab.browser.setAttribute(
        "oncontextmenu",
        "return mailContextOnContextMenu(event);"
      );
      aTab.browser.openWindowInfo = aArgs.openWindowInfo || null;
      clone.querySelector("stack").appendChild(aTab.browser);

      if (aArgs.skipLoad) {
        clone.querySelector("browser").setAttribute("nodefaultsrc", "true");
      }
      aTab.panel.setAttribute("id", "bmTabWrapper" + this.lastBmId);
      aTab.panel.appendChild(clone);
      aTab.root = clone;

      // Start setting up the browser.
      aTab.toolbar = aTab.panel.querySelector(".contentTabToolbar");
      aTab.backButton = aTab.toolbar.querySelector(".back-btn");
      aTab.backButton.addEventListener("command", () => aTab.browser.goBack());
      aTab.forwardButton = aTab.toolbar.querySelector(".forward-btn");
      aTab.forwardButton.addEventListener("command", () =>
        aTab.browser.goForward()
      );
      aTab.security = aTab.toolbar.querySelector(".contentTabSecurity");
      aTab.urlbar = aTab.toolbar.querySelector(".contentTabUrlbar > input");
      aTab.urlbar.value = aArgs.contentPage;

      ExtensionParent.apiManager.emit(
        "extension-browser-inserted",
        aTab.browser
      );

      // As we're opening this tab, showTab may not get called, so set
      // the type according to if we're opening in background or not.
      let background = "background" in aArgs && aArgs.background;
      aTab.browser.setAttribute("type", "content");
      if (background) {
        aTab.browser.removeAttribute("primary");
      } else {
        aTab.browser.setAttribute("primary", "true");
      }

      aTab.clickHandler =
        "clickHandler" in aArgs && aArgs.clickHandler
          ? aArgs.clickHandler
          : "specialTabs.defaultClickHandler(event);";
      aTab.browser.setAttribute("onclick", aTab.clickHandler);

      // Set this attribute so that when favicons fail to load, we remove the
      // image attribute and just show the default tab icon.
      aTab.tabNode.setAttribute("onerror", "this.removeAttribute('image');");

      aTab.browser.addEventListener("DOMLinkAdded", DOMLinkHandler);

      // Now initialise the find bar.
      aTab.findbar = document.createXULElement("findbar");
      aTab.findbar.setAttribute(
        "browserid",
        "bmTabBrowser" + this.lastBmId
      );
      clone.appendChild(aTab.findbar);

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
              if (!aArgs.openInBackGround) {
                win.focus();
              }
            }
          }, true);
      }

      // Create a filter and hook it up to our browser
      let filter = Cc[
        "@mozilla.org/appshell/component/browser-status-filter;1"
      ].createInstance(Ci.nsIWebProgress);
      aTab.filter = filter;
      aTab.browser.webProgress.addProgressListener(
        filter,
        Ci.nsIWebProgress.NOTIFY_ALL
      );

      // Wire up a progress listener to the filter for this browser
      aTab.progressListener = new tabProgressListener(aTab, false);
      aTab.progressListener.addProgressListener({
        onProgressChange: function() {},
        onProgressChange64: function() {},
        onLocationChange: function bm_onLocationChange(aWebProgress, aRequest, aLocationURI, aFlags) {
            gBMOverlay._logger.debug("onLocationChange:" + aLocationURI.spec + ", " + aFlags);
            try {
              let path = aLocationURI.pathQueryRef;
              gBMOverlay._logger.debug("path:" + path);
              if ((path && (path.indexOf("/login/index.html") == 0)
                || (aLocationURI.spec != "about:blank" && aLocationURI.spec.indexOf(aArgs.contentPage) != 0))) {
                let tabmail = document.getElementById("tabmail");
                tabmail.closeTab(aTab);
                delayOpener.open({
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

      filter.addProgressListener(
        aTab.progressListener,
        Ci.nsIWebProgress.NOTIFY_ALL
      );

      if ("onListener" in aArgs) {
        aArgs.onListener(aTab.browser, aTab.progressListener);
      }

      // Initialize our unit testing variables.
      aTab.pageLoading = false;
      aTab.pageLoaded = false;

      // Now start loading the content.
      aTab.title = this.loadingTabString;

      // BM tab id
      aTab.tabNode.setAttribute("bmApp", aArgs.bmApp);

      if (!aArgs.skipLoad) {
        let params = {
          triggeringPrincipal: Services.scriptSecurityManager.getSystemPrincipal(),
        };
        aTab.browser.loadURI(aArgs.contentPage, params);
      }

      this.lastBmId++;
    },
    tryCloseTab(aTab) {
      let docShell = aTab.browser.docShell;
      // If we have a docshell, a contentViewer, and it forbids us from closing
      // the tab, then we return false, which means, we can't close the tab. All
      // other cases return true.
      return !(
        docShell &&
        docShell.contentViewer &&
        !docShell.contentViewer.permitUnload()
      );
    },
    persistTab(aTab) {
      if (aTab.browser.currentURI.spec == "about:blank") {
        return null;
      }

      let onClick = aTab.clickHandler;

      return {
        tabURI: aTab.browser.currentURI.spec,
        clickHandler: onClick ? onClick : null,
        bmApp: aTab.tabNode.getAttribute("bmApp")
      };
    },
    restoreTab(aTabmail, aPersistedState) {
        delayOpener.open({
            bmApp: aPersistedState.bmApp,
            openInBackGround: true
        });
    }
};

function bmTabsDelayOpener() {
    this._apps = [];
    this._timer = null;
}

bmTabsDelayOpener.prototype = {
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
};

var delayOpener = new bmTabsDelayOpener();

// custom event, fired by the overlay loader after it has finished loading
document.addEventListener("DOMOverlayLoaded_bm-connector-tb@blue-mind.net", () => {
    console.log("bmContentTab loaded");
    let tabmail = document.getElementById("tabmail");
    tabmail.registerTabType(specialTabs.bmTabType);
}, { once: true });
