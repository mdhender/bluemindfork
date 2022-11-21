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

/* */

this.EXPORTED_SYMBOLS = ["BMContactHome", "BMContact"];

var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");

var { VCardProperties } = ChromeUtils.import("resource:///modules/VCardUtils.jsm");

function BMContactHome() {
    this._logger = Components.classes["@blue-mind.net/logger;1"].getService()
                            .wrappedJSObject.getLogger("BMContactHome: ");
}

function labelToTypeParams(label) {
    let params = [];
    let labels = label.split(",");
    for (let i = 0; i < labels.length; i++) {
        params.push({'label': 'TYPE', 'value': labels[i]});
    }
    return params;
}

function typeParamsToLabel(parameters) {
    let label = [];
    parameters.forEach(function(param) {
        if (param.label == "TYPE") {
            label.push(param.value);
        }
    });
    return label.toString();
}

function paramsHasDefault(parameters) {
    if (parameters) {
        for (let param of parameters) {
            if (param.label == "DEFAULT") {
                return param.value == "true";
            }
        }
    }
    return false;
}

BMContactHome.prototype.asEntry = function(/*BMContact*/ contact) {
    let entry = {};
    entry['id'] = contact.getId();
    entry['externalId'] = contact.getExtId();
    entry['name'] = contact.getDisplayName();
    entry['folder'] = contact.getFolder();
    let card = {
        'identification' : {},
        'explanatory' : {},
        'organizational' : {},
        'communications' : {},
        'deliveryAddressing' : [],
        'related' : {}
    };

    entry['value'] = card;
    card.kind = 'individual';

    card.identification.formatedName = {
        'value' : contact.getDisplayName()
    };

    card.identification.name = {
        'familyNames' : contact.getLastName(),
        'givenNames' : contact.getFirstName(),
        'additionalNames' : contact.getMiddleName(),
        'suffixes' : contact.getSuffix(),
        'prefixes' : contact.getTitle()
    }

    card.identification.nickname = {
        'value' : contact.getAka()
    };

    card.identification.gender = {
        'value': contact.getGender()
    };
    card.identification.birthday = contact.getBirth();
    card.identification.anniversary = contact.getAnniversary();
    
    card.identification.photo = contact.hasPhoto();
    
    card.explanatory = {
        'note' : contact.getNotes(),
        'urls' : []
    };
    
    let sites = contact.getWebSites();
    sites.forEach(function(site) {
        card.explanatory.urls.push({
            'value' : site.url,
            'parameters' : labelToTypeParams(site.label)
        });
    });

    card.organizational.title = contact.getJobTitle();
    card.organizational.role = contact.getRole();
    card.organizational.org = {
        'company' : contact.getCompany(),
        'department' : contact.getDepartment()
    };

    card.communications.tels = [];
    let phones = contact.getPhones();
    phones.forEach(function(phone) {
        card.communications.tels.push({
            'value' : phone.phone,
            'parameters' : labelToTypeParams(phone.label)
        });
    });

    card.communications.impps = [];
    let ims = contact.getIms();
    ims.forEach(function(im) {
        card.communications.impps.push({
            'value' : (im.protocol ? im.protocol + ":" : "") + im.id,
            'parameters' : labelToTypeParams(im.label)
        });
    });

    card.communications.emails = [];
    let emails = contact.getEmails();
    emails.forEach(function(mail) {
        card.communications.emails.push({
            'value' : mail.email,
            'parameters' : labelToTypeParams(mail.label)
        });
    });

    let addresses = contact.getAddresses();
    addresses.forEach(function(adr) {
        let cadr = {
            'streetAddress' : adr.street,
            'postOfficeBox' : adr.expresspostal,
            'locality' : adr.town,
            'postalCode' : adr.zipcode,
            'region' : adr.state,
            'countryName' : adr.country,
            'value': null,
            'parameters' : labelToTypeParams(adr.label)
        }
        card.deliveryAddressing.push({
            'address' : cadr
        });
    });

    card.explanatory.categories = [];
    let tags = contact.getTags();
    tags.forEach(function(tag) {
        card.explanatory.categories.push(tag)
    });
    
    card.related.assistant = contact.getAssistant();
    card.related.manager = contact.getManager();
    card.related.spouse = contact.getSpouse();
    
    return entry;
}

BMContactHome.prototype.fillContactFromEntry = function(entry, /*BMContact*/ contact) {
    this._logger.debug("fillContactFromEntry");
    contact.setId(entry['id']);
    contact.setExtId(entry['externalId']);
    contact.setFolder(entry['folder']);

    let card = entry['value'];

    let name = card.identification.name;
    if (name) {
        contact.setLastName(name.familyNames);
        contact.setFirstName(name.givenNames);
        contact.setMiddleName(name.additionalNames);
        contact.setSuffix(name.suffixes);
        contact.setTitle(name.prefixes);
    }
    
    if (card.identification.formatedName) {
        contact.setDisplayName(card.identification.formatedName.value);
    }

    if (card.identification.nickname) {
        contact.setAka(card.identification.nickname.value);
    }
    
    contact.setGender(card.identification.gender.value);
    contact.setBirth(card.identification.birthday);
    contact.setAnniversary(card.identification.anniversary);

    if (card.explanatory) {
        contact.setNotes(card.explanatory.note);

        let sites = [];
        card.explanatory.urls.forEach(function(url) {
            let label = typeParamsToLabel(url.parameters);
            sites.push({label:label, url:url.value});
        });
        contact.setWebsites(sites);

        if (card.explanatory.categories) {
            contact.setTags(card.explanatory.categories);
        }
    }

    if (card.organizational) {
        contact.setJobTitle(card.organizational.title);
        contact.setRole(card.organizational.role);

        if (card.organizational.org) {
            contact.setCompany(card.organizational.org.company);
            contact.setDepartment(card.organizational.org.department);
        }
    }

    if (card.communications) {
        let phones = [];
        card.communications.tels.forEach(function(tel) {
            let label = typeParamsToLabel(tel.parameters);
            phones.push({label: label, phone:tel.value});
        });
        contact.setPhones(phones);

        let ims = [];
        card.communications.impps.forEach(function(impp) {
            let label = typeParamsToLabel(impp.parameters);
            let pv = impp.value.split(":");
            let prot = null;
            let value;
            if (pv.length > 1) {
                prot = pv[0];
                value = pv[1];
            } else {
                prot = "xmpp";
                value = impp.value;
            }
            ims.push({label:label, id:value, protocol:prot});
        });
        contact.setIms(ims);

        let emails = [];
        let defFound = false;
        card.communications.emails.forEach(function(email) {
            let label = typeParamsToLabel(email.parameters);
            let eml = {label:label, email:email.value};
            if (!defFound && paramsHasDefault(email.parameters)) {
                emails.unshift(eml);
                defFound = true;
            } else {
                emails.push(eml);
            }
        });
        contact.setEmails(emails);
    }

    if (card.deliveryAddressing) {
        let addresses = [];
        card.deliveryAddressing.forEach(function(da) {
            if (da.address) {
                let adr = da.address;
                addresses.push({
                    label: typeParamsToLabel(adr.parameters),
                    street: adr.streetAddress,
                    zipcode: adr.postalCode,
                    town: adr.locality,
                    expresspostal: adr.postOfficeBox,
                    state: adr.region,
                    country: adr.countryName
                });
            }
        });
        contact.setAddresses(addresses);
    }
    
    if (card.related) {
        contact.setAssistant(card.related.assistant);
        contact.setManager(card.related.manager);
        contact.setSpouse(card.related.spouse);
    }

    return contact;
};

let imLabelByField = [];
imLabelByField["_GoogleTalk"] = "gtalk";
imLabelByField["_AimScreenName"] = "aim";
imLabelByField["_Yahoo"] = "ymsgr";
imLabelByField["_Skype"] = "skype";
imLabelByField["_QQ"] = "qq";
imLabelByField["_MSN"] = "msn";
imLabelByField["_ICQ"] = "icq";
imLabelByField["_JabberId"] = "xmpp";
imLabelByField["_IRC"] = "irc";

let imFieldByLabel = [];
imFieldByLabel["gtalk"] = "_GoogleTalk";
imFieldByLabel["aim"] = "_AimScreenName";
imFieldByLabel["ymsgr"] = "_Yahoo";
imFieldByLabel["skype"] = "_Skype";
imFieldByLabel["qq"] = "_QQ";
imFieldByLabel["msn"] = "_MSN";
imFieldByLabel["icq"] = "_ICQ";
imFieldByLabel["xmpp"] = "_JabberId";
imFieldByLabel["irc"] = "_IRC";

function BMContact(/*nsIAbCard*/ aCard) {
    let props = aCard.vCardProperties;
    if (props) {
        this._card = aCard.wrappedJSObject; 
        this._props = props.toPropertyMap();
        this._getProp = function(propName, defValue) {
            return this._props.has(propName) ? this._props.get(propName) : defValue;
        };
        this._setProp = function(propName, value) {
            if ([null, undefined, ""].includes(value)) {
                this._card._properties.delete(propName);
                return;
            }
            if (typeof value == "boolean") {
                value = value ? "1" : "0";
            }
            this._card._properties.set(propName, "" + value);
        };
        this.beforeSave = function() {
            // recalculate VCardProperties from modified properties
            this._card._vCardProperties = VCardProperties.fromPropertyMap(this._card._properties);
        };
    } else {
        this._card = aCard;
        this._getProp = function(propName, defValue) {
            return this._card.getProperty(propName, defValue);
        };
        this._setProp = function(propName, value) {
            this._card.setProperty(propName, value);
        };
        this.beforeSave = function() {};
    }
    this._logger = Components.classes["@blue-mind.net/logger;1"].getService()
                            .wrappedJSObject.getLogger("BMContact: ");
}

BMContact.prototype = {
    getId: function() {
        return this._getProp("bm-id", null);
    },
    setId: function(value) {
        this._setProp("bm-id", value);
    },
    getExtId: function() {
        return this._getProp("bm-extId", null);
    },
    setExtId: function(value) {
        this._setProp("bm-extId", value);
    },
    getFolder: function() {
        return this._getProp("bm-folder", null);
    },
    setFolder: function(value) {
        this._setProp("bm-folder", value);
    },
    getLastName: function() {
        return this._getProp("LastName", null);
    },
    setLastName: function(value) {
        this._card.lastName = value;
    },
    getFirstName: function() {
        return this._getProp("FirstName", null);
    },
    setFirstName: function(value) {
        this._card.firstName = value;
    },
    getDisplayName: function() {
        return this._getProp("DisplayName", null);
    },
    setDisplayName: function(value) {
        this._card.displayName = value;
    },
    getMiddleName: function() {
        return this._getProp("X-BM-middleName", null);
    },
    setMiddleName: function(value) {
        this._setProp("X-BM-middleName", value);
    },
    getSuffix: function() {
        return this._getProp("X-BM-suffix", null);
    },
    setSuffix: function(value) {
        this._setProp("X-BM-suffix", value);
    },
    getAka: function() {
        return this._getProp("NickName", null);
    },
    setAka: function(value) {
        this._setProp("NickName", value);
    },
    getGender: function() {
        return this._getProp("X-BM-gender", null); 
    },
    setGender: function(value) {
        this._setProp("X-BM-gender", value);
    },
    getNotes: function() {
        return this._getProp("Notes", null);
    },
    setNotes: function(value) {
        //FIXME tb do not suport HTML
        this._setProp("Notes", bmUtils.convertToPlainText(value));
    },
    getTitle: function() {
        return this._getProp("Title");
    },
    setTitle: function(value) {
        this._setProp("Title", value);
    },
    getCompany: function() {
        return this._getProp("Company");
    },
    setCompany: function(value) {
        this._setProp("Company", value); 
    },
    getJobTitle: function() {
        return this._getProp("JobTitle");
    },
    setJobTitle: function(value) {
        this._setProp("JobTitle", value);
    },
    getDepartment: function() {
        return this._getProp("Department", null);
    },
    setDepartment: function(value) {
        this._setProp("Department", value);
    },
    getRole: function() {
        return this._getProp("X-BM-role", null);
    },
    setRole: function(value) {
        this._setProp("X-BM-role", value);
    },
    getBirth: function() {
        let birth = null;
        if (this._getProp("BirthMonth", null)) {
            birth = new Date(Date.UTC(this._getProp("BirthYear", 1970),
                            this._getProp("BirthMonth", 1) - 1,
                            this._getProp("BirthDay", 1)));
            birth = birth.getTime();
        }
        return birth;
    },
    setBirth: function(value) {
        if (value) {
            let birth = new Date(value);
            this._setProp("BirthYear", birth.getFullYear());
            this._setProp("BirthMonth", birth.getMonth() + 1);
            this._setProp("BirthDay", birth.getDate());
        } else {
            this._setProp("BirthYear", "");
            this._setProp("BirthMonth", "");
            this._setProp("BirthDay", "");
        }
    },
    getAnniversary: function() {
        let anniversary = null;
        if (this._getProp("AnniversaryMonth", null)) {
            let anniv = new Date(Date.UTC(this._getProp("AnniversaryYear", 1970),
                                this._getProp("AnniversaryMonth", 1) - 1,
                                this._getProp("AnniversaryDay", 1)));
            anniversary = anniv.getTime();
        }
        return anniversary;
    },
    setAnniversary: function(value) {
        if (value) {
            let anniv = new Date(value);
            this._setProp("AnniversaryYear", anniv.getFullYear);
            this._setProp("AnniversaryMonth", anniv.getMonth() + 1);
            this._setProp("AnniversaryDay", anniv.getDate());
        } else {
            this._setProp("AnniversaryYear", "");
            this._setProp("AnniversaryMonth", "");
            this._setProp("AnniversaryDay", "");
        }
    },
    //FIXME not clear if there is 2 WebSites in tbird
    getWebSites: function() {
        let sites = [];
        let webPage1 = this._getProp("WebPage1", null);
        if (webPage1) {
            sites.push({url:webPage1, label:"work"});
        }
        let webPage2 = this._getProp("WebPage2", null); 
        if (webPage2) {
            sites.push({url:webPage2, label:"home"});
        }
        let extras = JSON.parse(this._getProp("X-BM-extraWebPages", "[]"));
        extras.forEach(function(extra) {
            sites.push(extra);
        });
        return sites;
    },
    setWebsites: function(value) {
        let site = null;
        if (value.length > 0) {
            site = value.shift().url;
        }
        this._setProp("WebPage1", site);
        site = null;
        if (value.length > 0) {
            site = value.shift().url;
        }
        this._setProp("WebPage2", site);
        this._setProp("X-BM-extraWebPages", JSON.stringify(value));
    },
    getPhones: function() {
        let phones = [];
        let p = this._getProp("WorkPhone", null);
        if (p != null) {
            phones.push({phone: p, label: "work,voice"})
        }
        p = this._getProp("HomePhone", null);
        if (p != null) {
            phones.push({phone: p, label: "home,voice"})
        }
        p = this._getProp("FaxNumber", null);
        if (p != null) {
            phones.push({phone: p, label: "work,fax"})
        }
        p = this._getProp("PagerNumber", null);
        if (p != null) {
            phones.push({phone: p, label: "pager"})
        }
        p = this._getProp("CellularNumber", null);
        if (p != null) {
            phones.push({phone: p, label: "cell,voice"})
        }
        let extras = JSON.parse(this._getProp("X-BM-extraPhones", "[]"));
        extras.forEach(function(extra) {
            phones.push(extra);
        });
        return phones;
    },
    setPhones: function(value) {
        let work = null;
        let home = null;
        let fax = null;
        let page = null;
        let mobile = null;
        let extras = [];
        for (let phone of value) {
            this._logger.debug("set phone: label:" + phone.label + " value:" + phone.phone);
            if (!work && this._containsTypes(phone.label, ["work","voice"])) {
                work = phone.phone;
            } else if (!home && this._containsTypes(phone.label, ["home","voice"])) {
                home = phone.phone;
            } else if (!fax && this._containsTypes(phone.label, ["work","fax"])) {
                fax = phone.phone;
            } else if (!page && this._containsTypes(phone.label, ["pager"])) {
                page = phone.phone;
            } else if (!mobile && this._containsTypes(phone.label, ["cell","voice"])) {
                mobile = phone.phone;
            } else {
                extras.push(phone);
            }
        }
        this._setProp("WorkPhone", work);
        this._setProp("HomePhone", home);
        this._setProp("FaxNumber", fax);
        this._setProp("PagerNumber", page);
        this._setProp("CellularNumber", mobile);
        this._setProp("X-BM-extraPhones", JSON.stringify(extras));
    },
    _containsTypes: function(label, types) {
        let labels = label.split(",");
        let res = types.every(function(type) {
            return labels.indexOf(type) != -1;
        });
        return res;
    },
    getIms: function() {
        let ims = [];
        for (let imField in imLabelByField) {
            let im = this._getProp(imField, null);
            if (im) {
                ims.push({id: im, protocol: imLabelByField[imField], label:""});
            }
        }
        let extras = JSON.parse(this._getProp("X-BM-extraIms", "[]"));
        extras.forEach(function(extra) {
            ims.push(extra);
        });
        return ims;
    },
    setIms: function(value) {
        let imValues = [];
        for (let imField in imLabelByField) {
            imValues[imField] = null;
        }
        let extras = [];
        for (let im of value) {
            this._logger.debug("set im:" + im.protocol + ":" + im.id);
            let field = imFieldByLabel[im.protocol];
            if (field && !imValues[field]) {
                imValues[field] = im.id;
            } else {
                extras.push(im);
            }
        }
        for (let imField in imValues) {
            this._setProp(imField, imValues[imField]);
        }
        this._setProp("X-BM-extraIms", JSON.stringify(extras));
    },
    getEmails: function() {
        let emails = [];
        let e = this._card.primaryEmail;
        if (e) {
            emails.push({label:"", email:e});
        }
        e = this._getProp("SecondEmail", null);
        if (e) {
            emails.push({label:"", email:e});
        }
        let extras = JSON.parse(this._getProp("X-BM-extraEmails", "[]"));
        extras.forEach(function(extra) {
            emails.push(extra);
        });
        return emails;
    },
    setEmails: function(value) {
        let email = null;
        if (value.length > 0) {
            email = value.shift().email;
        }
        this._card.primaryEmail = email;
        email = null;
        if (value.length > 0) {
            email = value.shift().email;
        }
        this._setProp("SecondEmail", email);
        this._setProp("X-BM-extraEmails", JSON.stringify(value));
    },
    getAddresses: function() {
        let addresses = [];
        
        function mergeStreet(s1, s2) {
            let ret = null;
            if (s1) ret = s1.trim();
            if (ret && s2) {
                ret += " " + s2.trim();
            } else if (s2) {
                ret = s2.trim();
            }
            return ret;
        }
        
        let addr = {};
        let street1 = this._getProp("HomeAddress", null);
        let street2 = this._getProp("HomeAddress2", null);
        addr.street = mergeStreet(street1, street2);
        addr.zipcode = this._getProp("HomeZipCode", null);
        addr.town = this._getProp("HomeCity", null);
        addr.expresspostal = this._getProp("X-BM-homeExpresspostal", null);
        addr.state = this._getProp("HomeState", null);
        addr.country = this._getProp("HomeCountry", null);
        addr.label = "home";
        
        if (addr.street || addr.zipcode || addr.town
            || addr.expresspostal || addr.state || addr.country) {
            addresses.push(addr);
        }
        
        addr = {};
        street1 = this._getProp("WorkAddress", null);
        street2 = this._getProp("WorkAddress2", null);
        addr.street = mergeStreet(street1, street2);
        addr.zipcode = this._getProp("WorkZipCode", null);
        addr.town = this._getProp("WorkCity", null);
        addr.expresspostal = this._getProp("X-BM-workExpresspostal", null);
        addr.state = this._getProp("WorkState", null);
        addr.country = this._getProp("WorkCountry", null);
        addr.label = "work";
        
        if (addr.street || addr.zipcode || addr.town
            || addr.expresspostal || addr.state || addr.country) {
            addresses.push(addr);
        }
        
        let extras = JSON.parse(this._getProp("X-BM-extraAddresses", "[]"));
        extras.forEach(function(extra) {
            addresses.push(extra);
        });
        
        return addresses;
    },
    setAddresses: function(value) {
        let home = null;
        let work = null;
        let extras = [];
        for (let addr of value) {
            if (addr.label == "home" && !home) {
                home = addr;
            } else if (addr.label == "work" && !work) {
                work = addr;
            } else {
                extras.push(addr);
            }
        }
        this._logger.debug("set home address: " + home);
        if (!home) {
            home = {street: null, zipcode: null, town: null, state: null, country: null, expresspostal: null};
        }
        this._setProp("HomeAddress", home.street);
        this._setProp("HomeZipCode", home.zipcode);
        this._setProp("HomeCity", home.town);
        this._setProp("HomeState", home.state);
        this._setProp("HomeCountry", home.country);
        this._setProp("X-BM-homeExpresspostal", home.expresspostal);
        
        this._logger.debug("set work address: " + work);
        if (!work) {
            work = {street: null, zipcode: null, town: null, state: null, country: null, expresspostal: null};
        }
        this._setProp("WorkAddress", work.street);
        this._setProp("WorkZipCode", work.zipcode);
        this._setProp("WorkCity", work.town);
        this._setProp("WorkState", work.state);
        this._setProp("WorkCountry", work.country);
        this._setProp("X-BM-workExpresspostal", work.expresspostal);
        
        this._setProp("X-BM-extraAddresses", JSON.stringify(extras));
    },
    getTags: function() {
        return JSON.parse(this._getProp("X-BM-tags", "[]"));
    },
    setTags: function(value) {
        this._setProp("X-BM-tags", JSON.stringify(value));
    },
    getAssistant: function() {
        return this._getProp("X-BM-assistant", null);
    },
    setAssistant: function(value) {
        this._setProp("X-BM-assistant", value);
    },
    getManager: function() {
        return this._getProp("X-BM-manager", null);
    },
    setManager: function(value) {
        this._setProp("X-BM-manager", value);
    },
    getSpouse: function() {
        return this._getProp("X-BM-spouse", null);
    },
    setSpouse: function(value) {
        this._setProp("X-BM-spouse", value);
    },
    hasPhoto: function() {
        let photoName = this._getProp("PhotoName", null);
        return photoName != null;
    },
}
