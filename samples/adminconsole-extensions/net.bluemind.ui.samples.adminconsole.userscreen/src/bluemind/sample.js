goog.provide('net.bluemind.ui.samples.adminconsole.userscreen.Contributor');
goog.provide('bm.sample.UserSettingsHelloworld');
goog.require('net.bluemind.ui.samples.adminconsole.userscreen.template');
goog.require('goog.soy');


net.bluemind.ui.samples.adminconsole.userscreen.Contributor.screenContributor = function() {
	return {
		"contribute" : function() {
			return [{
				  "contributedElementId" : "editUserGeneral",
				  "contributedAttribute" : "childrens",
				  "contribution" : {
				    "type" : "bm.sample.UserSettingsHelloworld"
				  }
				}];
		}
	}
};

/** 
 * @constructor
 */
bm.sample.UserSettingsHelloworld = function(model) {
	if( model && model['id']) {
		this['id'] = model['id'];
	}
	
	this['type'] = 'bm.sample.UserSettingsHelloworld';	
};

/**
 * @expose
 */
bm.sample.UserSettingsHelloworld.prototype.loadModel = function(model) {
	this.elt_.value = model['user-settings']['lang'];
};
/**
 * @expose
 */
bm.sample.UserSettingsHelloworld.prototype.saveModel = function(model) {
	model['user-settings']['lang'] = this.elt_.value;
};

/**
 * @expose
 */
bm.sample.UserSettingsHelloworld.prototype.attach = function(parent) {
	var el = goog.soy.renderAsElement(
			net.bluemind.ui.samples.adminconsole.userscreen.template.main, null);
	goog.dom.appendChild(parent, el);
	this.elt_ = goog.dom.getElementsByTagNameAndClass('input', 'myinput', el)[0];
};

goog.exportSymbol('bm.sample.UserSettingsHelloworld',bm.sample.UserSettingsHelloworld);
goog.exportSymbol('SampleUserScreenContributor',net.bluemind.ui.samples.adminconsole.userscreen.Contributor.screenContributor);
