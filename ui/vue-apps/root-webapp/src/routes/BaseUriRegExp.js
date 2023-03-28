const BaseUriRegExp = new RegExp("^" + new URL(document.baseURI).pathname.replace(/\/[^/]*$/, ""));
export default BaseUriRegExp;
