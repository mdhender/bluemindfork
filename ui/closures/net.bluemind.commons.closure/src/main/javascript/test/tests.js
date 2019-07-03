
goog.provide('bluemind.tests');


bluemind.tests.chain = function(funcs, value) {
	if (funcs.length == 0)
		return;

	if (funcs.length == 1) {
		return funcs[0].apply(null, [ value ]);
	}
	var res = funcs[0].apply(null, [ value ]);
	if (res === undefined) {
		goog.array.splice(funcs, 0, 1);
		bluemind.tests.chain(funcs, null);
	} else {
		res.addCallback(function(v) {
			goog.array.splice(funcs, 0, 1);
			bluemind.tests.chain(funcs, v);
		}).addErrback(function(e) {
			goog.array.splice(funcs, 0, 1);
			bluemind.tests.chain(funcs, e);
		});
	}
	;
}