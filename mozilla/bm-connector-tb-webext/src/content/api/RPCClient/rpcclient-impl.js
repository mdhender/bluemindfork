/* eslint-disable object-shorthand */

// Get various parts of the WebExtension framework that we need.
var { ExtensionCommon } = ChromeUtils.import("resource://gre/modules/ExtensionCommon.jsm");
var { Services } = ChromeUtils.import("resource://gre/modules/Services.jsm");

var RPCClientApi = class extends ExtensionCommon.ExtensionAPI {
  getAPI(context) {
    return {
      RPCClientApi: {
        init: async function () {

          var { XPCOMUtils } = ChromeUtils.import("resource://gre/modules/XPCOMUtils.jsm");

          let classID = Components.ID("{ce2954d3-624a-454d-bd1b-bc2facdd623b}");
          let contractID = "@blue-mind.net/rpcclient;1";

          Components.utils.importGlobalProperties(["XMLHttpRequest"]);
          let XMLHttpReqInScope = true;

          let logger = Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject
            .getLogger("RPCClient: ");

          function responseToString(response, aCommand) {
            if (response) {
              if (aCommand.url.indexOf("api/hsm") != -1)
                return "--mail data--";
              if (aCommand.url.indexOf("api/mailtip") != -1)
                return "--mail tips--";
              return response;
            }
            return "empty";
          }

          function sendDataToString(aCommand, aData) {
            if (aCommand.url.endsWith("photo"))
              return "--photo--";
            if (aCommand.url.indexOf("auth/login") != -1)
              return "******";
            if (aCommand.url.indexOf("login/native") != -1)
              return aData.replace(/password=.*?(?=&)/, "password=******");
            return aData;
          }

          function Command(aUrl, aMethod, aResponseType, aId) {
            this.id = aId;
            this.url = aUrl;
            this.method = aMethod;
            this.responseType = aResponseType;
            this.wrappedJSObject = this;
          }

          function RPCClient() {
            this._authKey = null;
            this.wrappedJSObject = this;
          }

          RPCClient.prototype = {
            classDescription: "RPCClient XPCOM Component",
            classID: classID,
            contractID: contractID,
            QueryInterface:
              ChromeUtils.generateQI([Components.interfaces.nsISupports]),
            _uuidGenerator: Components.classes["@mozilla.org/uuid-generator;1"].getService(Components.interfaces.nsIUUIDGenerator),
            newCommand: function (aUrl, aMethod, aResponseType) {
              let uuid = this._uuidGenerator.generateUUID().toString();
              let cmdId = uuid.substring(0, 3) + uuid.substring(uuid.length - 3);
              return new Command(aUrl, aMethod, aResponseType, cmdId);
            },
            setAuthKey: function (aAuthKey) {
              this._authKey = aAuthKey;
            },
            execute: function (aCommand) {
              let xmlHttp = this._getXmlHttp();
              xmlHttp.timeout = 30 * 1000; //30 s
              if (logger.isTraceEnabled) {
                logger.trace(aCommand.id + " Open request: [" + aCommand.method + "] " + aCommand.url);
              }
              xmlHttp.open(aCommand.method, aCommand.url, /*aSync*/ true);
              if (this._authKey != null) {
                logger.trace(aCommand.id + " X-BM-ApiKey: " + this._authKey);
                xmlHttp.setRequestHeader("X-BM-ApiKey", this._authKey);
              }
              if (aCommand.headers) {
                aCommand.headers.forEach(function (header) {
                  xmlHttp.setRequestHeader(header[0], header[1]);
                });
              }
              xmlHttp.responseType = aCommand.responseType;
              //xmlHttp.channel.notificationCallbacks = new BadCertHandler();
              xmlHttp.onload = function () {
                switch (xmlHttp.status) {
                  case 200:
                  case 201:
                  case 204:
                    if (logger.isTraceEnabled) {
                      logger.trace(aCommand.id + " => [" + xmlHttp.status + "] " + responseToString(xmlHttp.response, aCommand));
                    }
                    aCommand.onSuccess(xmlHttp);
                    break;
                  case 401:
                  case 403:
                  case 409:
                  case 400:
                  case 404:
                  case 500:
                  case 502:
                  default:
                    logger.error(aCommand.id + " => [" + xmlHttp.status + "] " + responseToString(xmlHttp.response, aCommand));
                    aCommand.onFailure(xmlHttp);
                    break;
                }
              };
              xmlHttp.ontimeout = function () {
                logger.error("Timeout");
                aCommand.onFailure(xmlHttp);
              };
              xmlHttp.onerror = function () {
                logger.error(aCommand.id + " Error: " + xmlHttp.status);
                aCommand.onFailure(xmlHttp);
              };
              let sendData = null;
              if (aCommand.method != "GET") {
                sendData = aCommand.getData();
                if (logger.isTraceEnabled && sendData) {
                  logger.trace(aCommand.id + " Send data: " + sendDataToString(aCommand, sendData));
                }
              }
              xmlHttp.send(sendData);
            },
            _getXmlHttp: function () {
              if (XMLHttpReqInScope) {
                return new XMLHttpRequest();
              }
              return Components.classes["@mozilla.org/xmlextras/xmlhttprequest;1"]
                .createInstance(Components.interfaces.nsIXMLHttpRequest);
            }
          };

          function BadCertHandler() { }

          BadCertHandler.prototype = {
            getInterface: function (iid) {
              return this.QueryInterface(iid);
            },
            QueryInterface: function (iid) {
              if (!iid.equals(Components.interfaces.nsIBadCertListener2)
                && !iid.equals(Components.interfaces.nsIInterfaceRequestor)
                && !iid.equals(Components.interfaces.nsISupports))
                throw Components.results.NS_ERROR_NO_INTERFACE;
              return this;
            },
            notifyCertProblem: function (socketInfo, status, targetSite) {
              logger.info("Notify certificate problem");
              if (!status) {
                return true;
              }
              let mailWindow = Services.wm.getMostRecentWindow("mail:3pane");
              let timerCallback = {
                notify: function (timer) {
                  let params = {
                    exceptionAdded: false,
                    prefetchCert: true,
                    location: targetSite
                  };
                  mailWindow.openDialog("chrome://pippki/content/exceptionDialog.xul", "", "chrome,centerscreen,modal", params);
                }
              };
              this.timer = Components.classes["@mozilla.org/timer;1"].createInstance(Components.interfaces.nsITimer);
              this.timer.initWithCallback(timerCallback, 1000, Components.interfaces.nsITimer.TYPE_ONE_SHOT);
              return true;
            }
          };

          console.trace("Register component");

          let factory = XPCOMUtils.generateNSGetFactory([RPCClient])(classID);
          // WARNING: this assumes that Thunderbird is already running, as
          // Components.manager.registerFactory will be unavailable for a few
          // milliseconds after startup.
          Components.manager.registerFactory(classID, "RPCClient", contractID,
            factory);
          context.callOnClose({
            close() {
              Components.manager.unregisterFactory(classID, factory);
            }
          });

        }
      }
    }
  }
};
