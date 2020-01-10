import isFunction from "lodash.isfunction";

var FN_ARGS = /^(function\s*[^(]*)?\(?\s*([^=)]*)\)?/m;
var FN_ARG_SPLIT = /,/;
var STRIP_COMMENTS = /((\/\/.*$)|(\/\*[\s\S]*?\*\/))/gm;

export function annotate(fn) {
    let $inject, fnText, argDecl;
    if (isFunction(fn)) {
        if (!($inject = fn.$inject)) {
            $inject = [];
            fnText = fn.toString().replace(STRIP_COMMENTS, "");
            argDecl = fnText.match(FN_ARGS);
            argDecl[2].split(FN_ARG_SPLIT).forEach(function(name) {
                if (name.length > 0) {
                    $inject.push(name.trim());
                }
            });
            fn.$inject = $inject;
        }
    }

    return $inject;
}
