//


var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");

var gBMAbNewCard = {
    onremove: function() {
        //GetDirectoryFromURI = GetDirectoryFromURIOriginal;
    }
}

/*GetDirectoryFromURIOriginal = GetDirectoryFromURI;
function GetDirectoryFromURI(uri) {
    console.log("GetDirectoryFromURI(" + uri + ")");
    if (uri.startsWith("moz-abdirectory://")) {
        return null;
    }
    let directory = MailServices.ab.getDirectory(uri);
    let wrap =  false;
    let readOnly = false;
    if (bmUtils.isBmList(directory)) {
        wrap = true;
        readOnly = bmUtils.isBmReadOnlyList(directory);
    } else if (bmUtils.isBmDirectory(directory)) {
        wrap = true;
        readOnly = bmUtils.isBmReadOnlyAddressbook(directory);
    }
    if (wrap) {
        let myDirectory = Components.classes["@blue-mind.net/bmdirwrapper;1"].createInstance().wrappedJSObject;
        myDirectory.mDirectory = directory;
        myDirectory.mReadOnly = readOnly;
        console.log("Return wrapped readOnly :" + readOnly + " directory");
        return myDirectory;
    } else {
        return directory;
    }
}*/