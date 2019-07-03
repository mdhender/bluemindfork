goog.provide('net.bluemind.ui.samples.adminconsole.newscreen.Contributor');
goog.provide('bm.sample.Helloworld');
goog.require('net.bluemind.ui.samples.adminconsole.newscreen.template');
goog.require('goog.soy');

net.bluemind.ui.samples.adminconsole.newscreen.Contributor.menusContributor = function() {
	return {
		'contribute' : function() {
			return {
				'sections' : [],
				'screens' : [ {
					"parentId" : "dir1",
					"contribution" : {
						"id" : "editPingouin",
						"name" : "Edit pingouin",
						"role" : "/user/update",
						"topLevel" : false
					}
				} ]
			};
		}
	};
};

net.bluemind.ui.samples.adminconsole.newscreen.Contributor.screenContributor = function() {
	return {
		"contribute" : function() {
			return [{
				  "contributedElementId" : null,
				  "contributedAttribute" : null,
				  "contribution" : {
				    "id" : "editPingouin",
				    "type" : "bm.sample.Helloworld"
				  }
				}];
		}
	}
};

/** 
 * @constructor
 */
bm.sample.Helloworld = function(model) {
	if( model && model['id']) {
		this['id'] = model['id'];
	} else {
		this['id'] = 'editPingouin';
	}
	this['type'] = 'bm.sample.Helloworld';
	this['modelHandlers'] = [];
	this['overlay'] = true;
	this['sizehint'] = {'width': 200, 'height': 200};
};

/**
 * @expose
 */
bm.sample.Helloworld.prototype.loadModel = function() {
	
};
/**
 * @expose
 */
bm.sample.Helloworld.prototype.saveModel = function() {
	
};

/**
 * @expose
 */
bm.sample.Helloworld.prototype.save = function() {}
/**
 * @expose
 */
bm.sample.Helloworld.prototype.load = function() {}
/**
 * @expose
 */
bm.sample.Helloworld.prototype.attach = function(parent) {
	var el = goog.soy.renderAsElement(
			net.bluemind.ui.samples.adminconsole.newscreen.template.main, null);
	goog.dom.appendChild(parent, el);
};

goog.exportSymbol('bm.sample.Helloworld',bm.sample.Helloworld);
goog.exportSymbol('SampleMenusContributor',net.bluemind.ui.samples.adminconsole.newscreen.Contributor.menusContributor);
goog.exportSymbol('SampleScreenContributor',net.bluemind.ui.samples.adminconsole.newscreen.Contributor.screenContributor);
