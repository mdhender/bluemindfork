/* BEGIN LICENSE 
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
goog.provide("net.bluemind.contact.vcard.VCardModelAdapter");

goog.require("goog.array");
goog.require("net.bluemind.addressbook.api.i18n.Address.Caption");
goog.require("net.bluemind.addressbook.api.i18n.Email.Caption");
goog.require("net.bluemind.addressbook.api.i18n.IMPP.Caption");
goog.require("net.bluemind.addressbook.api.i18n.Tel.Caption");
goog.require("net.bluemind.addressbook.api.i18n.URL.Caption");

net.bluemind.contact.vcard.VCardModelAdapter = function(ctx) {
  this.ctx = ctx;
}

net.bluemind.contact.vcard.VCardModelAdapter.prototype.toModelView = function(vcard, addressbook) {
  var mv = {
    container : {
      id : addressbook['uid'],
      name : addressbook['name']
    },
    id : vcard['uid'],
    emails : [],
    tels : [],
    addresses : [],
    urls : [],
    impps : [],
    members : [],
    photo : null,
    hasPhoto : false
  }

  if (vcard['value']['identification']['photo']) {
    mv.photo = '/api/addressbooks/' + addressbook['id'] + '/' + vcard['id'] + '/photo';
    mv.photo = '/api/addressbooks/' + addressbook['uid'] + '/' + vcard['uid'] + '/photo';
    mv.hasPhoto = true;
  }

  if (vcard['value']['identification']['formatedName']) {
    mv.name = vcard['value']['identification']['formatedName']['value'];
  }
  if (vcard['value']['identification']['name']) {
    mv.fullname = {
      prefixes : vcard['value']['identification']['name']['prefixes'],
      firstnames : vcard['value']['identification']['name']['givenNames'],
      additionalNames : vcard['value']['identification']['name']['additionalNames'],
      lastnames : vcard['value']['identification']['name']['familyNames'],
      suffixes : vcard['value']['identification']['name']['suffixes']
    }
  }
  if (vcard['value']['identification']['nickname']) {
    mv.nickname = vcard['value']['identification']['nickname']['value']
  }
  if (vcard['value']['identification']['birthday']) {
    mv.birthday = this.ctx.helper('dateformat').formatter.date.format(new Date(vcard['value']['identification']['birthday']));
  }
  if (vcard['value']['identification']['anniversary']) {
    mv.anniversary = this.ctx.helper('dateformat').formatter.date.format(new Date(vcard['value']['identification']['anniversary']));
  }
  if (vcard['value']['organizational']['org']) {
    mv.company = vcard['value']['organizational']['org']['company'];
    mv.division = vcard['value']['organizational']['org']['division'];
    mv.department = vcard['value']['organizational']['org']['department'];
  }
  if (vcard['value']['organizational']['role']) {
    mv.role = vcard['value']['organizational']['role'];
  }
  if (vcard['value']['organizational']['title']) {
    mv.title = vcard['value']['organizational']['title'];
  }
  if (vcard['value']['related']['manager']) {
    mv.manager = vcard['value']['related']['manager'];
  }
  if (vcard['value']['related']['assistant']) {
    mv.assistant = vcard['value']['related']['assistant'];
  }
  if (vcard['value']['related']['spouse']) {
    mv.spouse = vcard['value']['related']['spouse'];
  }
  if (vcard['value']['explanatory']['note']) {
    mv.note = vcard['value']['explanatory']['note'];
  }
  if (vcard['value']['security']['key']) {
    mv.pemCertificate = vcard['value']['security']['key']['value'];
  }
  if (goog.isArray(vcard['value']['communications']['emails'])) {
    mv.emails = goog.array.map(vcard['value']['communications']['emails'], function(coordinate) {
      var label = this.typesToMV_(coordinate['parameters']);
      return {
        label : label,
        isDefault: this.is_('DEFAULT', coordinate['parameters']),
        i18n : net.bluemind.addressbook.api.i18n.Email.Caption.ALL[label]
            || net.bluemind.addressbook.api.i18n.Email.Caption.FALLBACK,
        value : coordinate['value']
      };
    }, this);
  }
  if (goog.isArray(vcard['value']['communications']['tels'])) {
    mv.tels = goog.array.map(vcard['value']['communications']['tels'], function(coordinate) {
      var label = this.typesToMV_(coordinate['parameters']);
      return {
        label : label,
        isDefault: this.is_('DEFAULT',coordinate['parameters']),
        i18n : net.bluemind.addressbook.api.i18n.Tel.Caption.ALL[label]
            || net.bluemind.addressbook.api.i18n.Tel.Caption.FALLBACK,
        value : coordinate['value']
      };
    }, this);
  }
  if (goog.isArray(vcard['value']['deliveryAddressing'])) {
    mv.addresses = goog.array.map(vcard['value']['deliveryAddressing'], function(deliveryAddressing) {
      var address = deliveryAddressing['address'];
      var label = this.typesToMV_(address['parameters']);
      return {
        label : label,
        isDefault: this.is_('DEFAULT', address['parameters']),
        i18n : net.bluemind.addressbook.api.i18n.Address.Caption.ALL[label]
            || net.bluemind.addressbook.api.i18n.Address.Caption.FALLBACK,
        value : {
          label : address['value'],
          street : address['streetAddress'],
          extentedaddress : address['extentedAddress'],
          postalcode : address['postalCode'],
          locality : address['locality'],
          pobox : address['postOfficeBox'],
          region : address['region'],
          country : address['countryName']

        }
      };
    }, this);
  }
  if (goog.isArray(vcard['value']['explanatory']['urls'])) {
    mv.urls = goog.array.map(vcard['value']['explanatory']['urls'], function(coordinate) {
      var label = this.typesToMV_(coordinate['parameters']);
      return {
        label : label,
        isDefault: this.is_('DEFAULT', coordinate['parameters']),
        i18n : net.bluemind.addressbook.api.i18n.URL.Caption.ALL[label]
            || net.bluemind.addressbook.api.i18n.URL.Caption.FALLBACK,
        value : coordinate['value']
      };
    }, this);
  }
  if (goog.isArray(vcard['value']['communications']['impps'])) {
    mv.impps = goog.array.map(vcard['value']['communications']['impps'], function(coordinate) {
      var label = this.typesToMV_(coordinate['parameters']);
      return {
        label : label,
        isDefault: this.is_('DEFAULT', coordinate['parameters']),
        i18n : net.bluemind.addressbook.api.i18n.IMPP.Caption.ALL[label]
            || net.bluemind.addressbook.api.i18n.IMPP.Caption.FALLBACK,
        value : coordinate['value']
      };
    }, this);
  }
  if (goog.isArray(vcard['value']['organizational']['member'])) {
    mv.members = goog.array.map(vcard['value']['organizational']['member'], function(member) {
      return {
        id : member['itemUid'],
        container : member['containerUid'],
        name : member['commonName'],
        email : member['mailto'],
        photo : '/api/addressbooks/' + member['containerUid'] + '/' + member['itemUid'] + '/icon'
      };
    }, this);
  }

  if (goog.isArray(vcard['value']['explanatory']['categories'])) {
    mv.categories = goog.array.map(vcard['value']['explanatory']['categories'], function(tag) {
      return {
        id : tag['itemUid'],
        container : tag['containerUid'],
        label : tag['label'],
        color : tag['color']
      };
    }, this);
  }

  return mv;
}

net.bluemind.contact.vcard.VCardModelAdapter.prototype.fromModelView = function(mv) {
  var vcard = {
    'container' : mv.container.id,
    'uid' : mv.id,
    'value' : {
      'identification' : {},
      'organizational' : {},
      'related' : {},
      'explanatory' : {},
      'communications' : {}
    }
  };
  if (mv.name) {
    vcard['value']['identification']['formatedName'] = {
      'value' : mv.name
    };
  }
  if (mv.fullname) {
    vcard['value']['identification']['name'] = {
      'prefixes' : mv.fullname.prefixes,
      'givenNames' : mv.fullname.firstnames,
      'additionalNames' : mv.fullname.additionalNames,
      'familyNames' : mv.fullname.lastnames,
      'suffixes' : mv.fullname.suffixes
    };
  }
  if (mv.nickname) {
    vcard['value']['identification']['nickname'] = {
      'value' : mv.nickname
    };
  }
  if (mv.birthday) {
    vcard['value']['identification']['birthday'] = mv.birthday.getTime(this.ctx.helper('timezone').getUTC());
  }
  if (mv.anniversary) {
    vcard['value']['identification']['anniversary'] = mv.anniversary.getTime(this.ctx.helper('timezone').getUTC());
  }
  if (mv.role) {
    vcard['value']['organizational']['role'] = mv.role;
  }
  if (mv.title) {
    vcard['value']['organizational']['title'] = mv.title;
  }
  if (mv.company || mv.division || mv.department) {
    vcard['value']['organizational']['org'] = {
      'company' : mv.company,
      'division' : mv.division,
      'department' : mv.department
    };
  }
  if (mv.manager) {
    vcard['value']['related']['manager'] = mv.manager;
  }
  if (mv.assistant) {
    vcard['value']['related']['assistant'] = mv.assistant;
  }
  if (mv.spouse) {
    vcard['value']['related']['spouse'] = mv.spouse;
  }
  if (mv.note) {
    vcard['value']['explanatory']['note'] = mv.note;
  }
  if (mv.emails.length > 0) {
    vcard['value']['communications']['emails'] = goog.array.map(mv.emails, function(email) {
      return {
        'parameters' : this.mvToTypes(email.label),
        'value' : email.value
      };
    }, this);
  }
  if (mv.tels.length > 0) {
    vcard['value']['communications']['tels'] = goog.array.map(mv.tels, function(tel) {
      return {
        'parameters' : this.mvToTypes(tel.label, "TEL"),
        'value' : tel.value
      };
    }, this);
  }
  
  if (mv.pemCertificate) {
    vcard['value']['security'] = {
      'key' : { 
        'parameters' : [],
        'value' : mv.pemCertificate
        }
    };
  }
  
  if (mv.addresses.length > 0) {
    vcard['value']['deliveryAddressing'] = goog.array.map(mv.addresses, function(address) {
      return {
        'address' : {
          'parameters' : this.mvToTypes(address.label),
          'value' : address.value.label,
          'streetAddress' : address.value.street,
          'extentedAddress' : address.value.extendedstreet,
          'postalCode' : address.value.postalcode,
          'locality' : address.value.locality,
          'postOfficeBox' : address.value.pobox,
          'region' : address.value.region,
          'countryName' : address.value.country
        }
      };
    }, this);
  }
  if (mv.urls.length > 0) {
    vcard['value']['explanatory']['urls'] = goog.array.map(mv.urls, function(url) {
      return {
        'parameters' : this.mvToTypes(url.label),
        'value' : url.value
      };
    }, this);
  }
  if (mv.impps.length > 0) {
    vcard['value']['communications']['impps'] = goog.array.map(mv.impps, function(impp) {
      return {
        'parameters' : this.mvToTypes(impp.label),
        'value' : impp.value
      };
    }, this);
  }

  vcard['value']['explanatory']['categories'] = goog.array.map(mv.categories, function(tag) {
    return {
      'itemUid' : tag.id,
      'containerUid' : tag.container,
      'label' : tag.label,
      'color' : tag.color
    };

  });

  if (mv.members != null) {
    vcard['value']['organizational']['member'] = goog.array.map(mv.members, function(m) {
      return {
        'commonName' : m.name,
        'mailto' : m.email,
        'containerUid' : m.container,
        'itemUid' : m.id
      };
    });
  }

  vcard['value']['identification']['photo'] = mv.hasPhoto;
  return vcard;
  // 'name' : mv.fullname.value,
  // 'kind' : 'individual',
}

/**
 * @param {Array} parameters
 * @param {*} labels
 * @return {Array}
 * @protected
 */
net.bluemind.contact.vcard.VCardModelAdapter.prototype.typesToMV_ = function(parameters) {
  parameters = parameters || [];
  var types = [];
  for (var i = 0; i < parameters.length; i++) {
    if (parameters[i]['label'] == 'TYPE') {
      types.push(parameters[i]['value']);
    }
  }
  goog.array.sort(types);
  var mv = types.join(',');
  return mv;
};

/**
 * @param {Array} parameters
 * @return {boolean}
 * @protected
 */
net.bluemind.contact.vcard.VCardModelAdapter.prototype.is_ = function(label, parameters) {
  return goog.array.find(parameters, function(parameter) {
    return (parameter['label'] == label && parameter['value'] == 'true');
  }) || false;
}
/**
 * @param {Array} parameters
 * @return {Array}
 * @protected
 */
net.bluemind.contact.vcard.VCardModelAdapter.prototype.mvToTypes = function(types, labelType) {
  if (types && types.split(',').length > 0) {
    return goog.array.map(types.split(','), function(type) {
      return {
        'label' : 'TYPE',
        'value' : type.toLowerCase()
      }
    });
  } else {
    if (null != labelType && labelType == 'TEL') {
      return [ {
        'label' : 'TYPE',
        'value' : 'work'
      }, {
        'label' : 'TYPE',
        'value' : 'voice'
      } ];
    } else {
      return [ {
        'label' : 'TYPE',
        'value' : 'work'
      } ];
    }
  }
};
