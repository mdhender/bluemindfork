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

var { Services } = ChromeUtils.import("resource://gre/modules/Services.jsm");
var { cloudFileAccounts } = ChromeUtils.import("resource:///modules/cloudFileAccounts.jsm");
var { FileUtils } = ChromeUtils.import("resource://gre/modules/FileUtils.jsm");

var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");
var { BMAuthService } = ChromeUtils.import("chrome://bm/content/modules/core2/BMAuthService.jsm");

try {
    var { MailE10SUtils } = ChromeUtils.import("resource:///modules/MailE10SUtils.jsm");
} catch(e) {
    //TB 78
}

Services.scriptloader.loadSubScript("chrome://bm/content/notifyTools.js", null, "UTF-8");

function canAttachFilesFromHosting() {
    let accs = cloudFileAccounts.getAccountsForType("BlueMind");
    if (accs.length > 0) {
        return accs[0].wrappedJSObject.canUseFilehosting();
    }
    return false;
}

function attachFilesFromHosting() {
    gBMRemoteChooser.loadWindow();
}

function onRemoteFileChoosed(aFiles) {
    /*files.push({
        size: size,         
        path: links[i].path, 
        name: links[i].name,
      });*/
    let accounts = cloudFileAccounts.getAccountsForType("BlueMind");
    if (accounts.length == 0)
        return;
    // get wrappedJSObject to acces methods not in the interface
    let provider = accounts[0].wrappedJSObject;
    
    for (let choosed of aFiles) {
        let file = FileUtils.getFile("TmpD", choosed.path.split("/"));
        if (!file.exists())
            file.createUnique(Components.interfaces.nsIFile.NORMAL_FILE_TYPE, FileUtils.PERMS_FILE);
        
        let attachment = FileToAttachment(file);
        
        attachment.name = choosed.name;
        attachment.size = choosed.size;
        attachment.sendViaCloud = true;
        attachment.cloudFileAccountKey = provider.accountKey;
        let listener = {
            onStartRequest: function() {},
            onStopRequest: function(p, ctx, cr) {
                if (!Components.isSuccessCode(cr)) {
                    return;
                }
                attachment.contentLocation = provider.urlForFile(file);
                AddAttachments([attachment], function(item) {
                    item.account = provider;
                    item.setAttribute("name", file.leafName);
                    item.image = provider.iconURL;
                    item.cloudFileUpload = {};
                    item.dispatchEvent(
                        new CustomEvent("attachment-uploaded", { bubbles: true, cancelable: true })
                    );
                });
            }
        }
        provider.shareFile(file, listener);
    }
}

bmUtils.overrideBM(gCloudAttachmentLinkManager, "_insertItem", function(original) {
  return function(aDocument, aAttachment, aProvider) {
    original.apply(this, arguments);
    if (aProvider.type != "BlueMind") {
        return;
    }
    if (gMsgCompose.composeHTML) {
        let bmProvider = aProvider.wrappedJSObject;
        let expireDate = bmProvider.expireForLink(aAttachment.contentLocation);
        
        let list = aDocument.getElementById("cloudAttachmentList");
        
        removeCloudAttachmentFooter(aDocument);
    
        if (expireDate && expireDate > 0) {
            let expire = aDocument.createElement("span");
            expire.textContent = bmUtils.getLocalizedString("filehosting.expireOn")
                                + " " + new Date(expireDate).toLocaleString();
            expire.style.marginLeft = "5px";
            expire.style.fontSize = "small";
            expire.style.color = "grey";
            
            let item = list.lastChild;
            let size = item.getElementsByTagName("span")[0];
            item.insertBefore(expire, size.nextSibling);
        }
    }
  }
});
    
function removeCloudAttachmentFooter(aDocument) {
    let rootList = aDocument.getElementById("cloudAttachmentListRoot");
    let footer = rootList.lastChild;
    if (footer && footer.style && footer.style.color == "rgb(68, 68, 68)") {
        footer.innerHTML = "";
    }
}

// net.bluemind.mailmessage.api.MailTipContext
function bmMailtipContext() {
    return {
        messageContext :  {
            fromIdentity: {
                sender: "",
                from: ""
            },
            recipients: []
        },
        filter :  {
            mailTips: ["Signature"],
            filterType: "INCLUDE"
        }
    }
}


var gBMCompose = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMCompose: "),
    _timer: Components.classes["@mozilla.org/timer;1"].createInstance(Components.interfaces.nsITimer),
    init: function() {
        let loader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"].getService(Components.interfaces.mozIJSSubScriptLoader);
        loader.loadSubScript("chrome://bm/content/core2/client/MailTipClient.js");
        loader.loadSubScript("chrome://bm/content/core2/client/UserMailIdentitiesClient.js");
    },
    checkSignature: function() {
        this._timer.cancel();
        this._timer.init(function() {
            let compFields = Components.classes["@mozilla.org/messengercompose/composefields;1"]
                                    .createInstance(Components.interfaces.nsIMsgCompFields);
            Recipients2CompFields(compFields);
            this._updateSignaturePreview(compFields);
        }.bind(this), 500, Components.interfaces.nsITimer.TYPE_ONE_SHOT);
    },
    _logged: null,
    _mailTipsCache: new HashMap(),
    _currentContextHash: null,
    _updateSignaturePreview: function(aMsgCompFields) {
        let self = this;

        let sender = gCurrentIdentity.email;
        this._checkSender(sender).then(function() {
            let recipients = self._getRecipients(aMsgCompFields);
            if (recipients && recipients.length > 0) {
                let from = aMsgCompFields.from ? aMsgCompFields.from : sender;
                let context = self._buildMailContext(recipients, sender, from);
                if (!self._isCurrentContext(context)) {
                    self._getMailTips(context).then(function(mailTips) {
                        self._toggleSignaturePreview(mailTips);
                        self._currentContextHash = self._hashContext(context);

                    });
                }
            }
        }).catch(function(e) {
            self._logger.info(e)
            self._hideSignature();
        })

    },
    _hashContext: function(aMailtipContext) {
        let hashCode = function(string) {
            var hash = 0, i, chr;
            if (string.length === 0) return hash;
            for (i = 0; i < string.length; i++) {
              chr   = string.charCodeAt(i);
              hash  = ((hash << 5) - hash) + chr;
              hash |= 0; // Convert to 32bit integer
            }
            return hash;
        };
        if (! aMailtipContext.hashCode) {
            let domains = aMailtipContext.messageContext.recipients.map(function(recipient) {
                return recipient.email.split('@').pop().toLowerCase();
            });
            domains = domains.filter(function(domain, position) {
                return domains.indexOf(domain) == position;
            }).sort();
            let hs = hashCode(aMailtipContext.messageContext.fromIdentity.sender) + ':' + hashCode(domains.join(','));
            aMailtipContext.hashCode = function() { return hs;}
        }
        return aMailtipContext.hashCode();
    },

    _isCurrentContext: function(aMailtipContext) {
        
        return this._currentContextHash === this._hashContext(aMailtipContext);
    },

    _buildMailContext: function(aRecipents, aSender, aFrom) {
        let mailtipContext = bmMailtipContext();
        mailtipContext.messageContext.fromIdentity.sender = aSender;
        mailtipContext.messageContext.fromIdentity.from = aFrom;
        mailtipContext.messageContext.recipients = aRecipents;
        return mailtipContext;
        
    },
    _getRecipients: function(aMsgCompFields) {
        expandRecipients();
        let recipients = [];
        for (let type of ["to", "cc", "bcc"]) {
          let emails = aMsgCompFields.splitRecipients(aMsgCompFields[type], /*email only*/true, {});
          for (let email of emails) {
            if (isValidAddress(email)) {
                let domain = email.split('@').pop();
                if (/^.+\..{2,}$/.test(domain)) {
                    recipients.push(this._toRecipient(type, email));
                    continue;
                }
            }
            return false;
          }
        }
        return recipients;
    },
    _checkSender: function(aEmail) {
        let auth, user = {}, pwd = {}, srv = {};
        if (!bmUtils.getSettings(user, pwd, srv, false)) {
            auth = Promise.reject();
        } else if (this._logged) {
            auth = Promise.resolve(this._logged);
        } else {
            auth = BMAuthService.login(srv.value, user.value, pwd.value);
        }
        let self = this;
        return auth.then(function(logged) {
            self._logged = logged;
            let idClient = new UserMailIdentitiesClient(srv.value, logged.authKey, logged.authUser.domainUid, logged.authUser.uid);
            return idClient.getIdentities();
        }).then(function(indentities) {
            for (let identity of indentities) {
                if (aEmail == identity.email) {
                    return;
                }
            }
            throw new Error("Sender: " + aEmail + " is not an email of configured user identities");
        });
    },
    _toRecipient: function(aType, aEmail) {
        let recip = {
            email: aEmail,
            addressType: "SMTP",
            recipientType: aType.toUpperCase()
        };
        return recip;
    },
    _getMailTips: function(aMailtipContext) {
        let user = {};
        let pwd = {};
        let srv = {};
        let contextHash = this._hashContext(aMailtipContext);
        if (this._mailTipsCache.containsKey(contextHash)) {
            return Promise.resolve(this._mailTipsCache.get(contextHash));
        }
        let self = this;
        if (bmUtils.getSettings(user, pwd, srv, false)) {
            let auth = BMAuthService.login(srv.value, user.value, pwd.value);
            return auth.then(function(logged) {
                let tips = new MailTipClient(srv.value, logged.authKey, logged.authUser.domainUid);
                return tips.getMailTips(aMailtipContext);
            }).then(function(mailTips) {
                self._mailTipsCache.put(contextHash, mailTips);
                return mailTips;
            });
        }
        return Promise.reject();
    },
    _toggleSignaturePreview: function(aMailTips) {
        if (aMailTips && aMailTips.length > 0) {
            let html = this._getSignaturesHtml(aMailTips);
            if (html) {
                this._showSignatures(html);
                return;
            }
        } 
        this._hideSignature();
        
    },
    _getSignaturesHtml: function(mailTips) {
        let divs = "";
        for (mailTip of mailTips) {
            for (tip of mailTip.matchingTips) {
                let signature = JSON.parse(tip.value);
                if (signature.html) {
                    divs += "<div>";
                    divs += signature.html;
                    divs += "</div>";
                }
            }
        }
        if (divs) {
            return "<html><head><meta charset=\"utf-8\"></head><body>" + divs + "</body></html>";
        }
        return null;
    },
    _showSignatures: function(aHtml) {
        if (MailE10SUtils) {
            let box = document.getElementById("bmSignature");
            box.setAttribute("collapsed", "false");
            let browser = document.getElementById("bm-browser-signature");
            let preview = "data:text/html," + encodeURIComponent(aHtml);
            MailE10SUtils.loadURI(browser, preview);
        } else {
            let tempFile = Components.classes["@mozilla.org/file/directory_service;1"]
                        .getService(Components.interfaces.nsIProperties)
                        .get("TmpD", Components.interfaces.nsIFile);
            tempFile.append("signatures.html");
            tempFile.createUnique(0, 0o600);
            let encoder = new TextEncoder();
            let byteArray = encoder.encode(aHtml);
            let prom = OS.File.writeAtomic(tempFile.path, byteArray);
            let self = this;
            prom.then(function() {
                let extService = Components.classes['@mozilla.org/uriloader/external-helper-app-service;1']
                    .getService(Components.interfaces.nsPIExternalAppLauncher);
                extService.deleteTemporaryFileOnExit(tempFile);
                let uri = Services.io.newFileURI(tempFile);

                let box = document.getElementById("bmSignature");
                box.setAttribute("collapsed", "false");
                let browser = document.getElementById("bm-browser-signature");
                if (!Components.interfaces.nsIMsgCloudFileProvider) {
                    // TB 68 loadURI extra param
                    let params = {
                    triggeringPrincipal: Services.scriptSecurityManager.getSystemPrincipal()
                    };
                    browser.loadURI(uri.spec, params);
                } else {
                    browser.loadURI(uri.spec);
                }
                if (bmUtils.session.sigPreviewClosed) {
                    self._showPreview(false);
                }
        });
        }
    },
    _hideSignature: function() {
        let box = document.getElementById("bmSignature");
        box.setAttribute("collapsed", "true");
    },
    togglePreview: function() {
        let bro = document.getElementById("bm-browser-signature");
        if (bro.getAttribute("collapsed") == "false") {
            this._showPreview(false);
        } else {
            this._showPreview(true);
        }
    },
    _showPreview: function(visible) {
        let bro = document.getElementById("bm-browser-signature");
        let toggle =  document.getElementById("bm-toggle-signature");
        bro.setAttribute("collapsed", visible ? "false" : "true");
        toggle.setAttribute("value", bmUtils.getLocalizedString("signature.show"));
        bmUtils.session.sigPreviewClosed = !visible;
    }
};

/* add bm autocomplete source when online and connector confed */
function BmAddAutocomplete() {
    let inuptFields = ["toAddrInput", "ccAddrInput", "bccAddrInput", "replyAddrInput"];
    for (let inputField of inuptFields) {
        let input = document.getElementById(inputField);
        if (input) {
            let value = input.getAttribute("autocompletesearch");
            if (value && value.length > 0) {
                value = value.replace(/(^| )addrbook($| )/, "$1addrbook bm-search$2");
                //value = value.replace(/(^| )addrbook($| )/, "$1bm-search$2");
                input.setAttribute("autocompletesearch", value);
            }
        }
    }
}

function BmInitCompose() {
    if (!Services.io.offline && bmUtils.getSettings({}, {}, {}, false)) {
        BmAddAutocomplete();
        let button = document.getElementById("button-attachPopup_BlueMind");
        button.setAttribute("hidden", !canAttachFilesFromHosting());
    }
    gBMCompose.init();
    gBMCompose.checkSignature();
    bmUtils.overrideBM(window, "onRecipientsChanged", function(original) {
        return function(aAutomatic) {
            original.apply(this, arguments);
            if (!aAutomatic) {
                gBMCompose.checkSignature();
            }
        }
    });
    window.addEventListener("compose-from-changed", function() {
        console.log("compose-from-changed");
        gBMCompose.checkSignature();
    });

    notifyTools.registerListener((data) => {
        switch (data.command) {
            case "onRemotechooserSuccess":
                onRemoteFileChoosed(data.files);
                break;
        }
    });
    notifyTools.enable();
}
