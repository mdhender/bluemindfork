import UUIDGenerator from "@bluemind/uuid";

import EmailExtractor from "./EmailExtractor";
import EmailValidator from "./EmailValidator";
import Flag from "./Flag";
import InlineImageHelper, { CID_DATA_ATTRIBUTE } from "./InlineImageHelper";
import mailText2Html from "./mailText2Html";
import MimeType from "./MimeType";
import PartsBuilder from "./PartsBuilder";

const USED_QUOTA_PERCENTAGE_WARNING = 80; // quota usage is considered as "to be watched" if it's more than 80%

const WEBSERVER_HANDLER_BASE_URL = "part/url/";

function getPartUrl(folderUid, imapUid, { address, charset, encoding, mime }) {
    const url = new URL(WEBSERVER_HANDLER_BASE_URL, document.baseURI);
    url.searchParams.set("folderUid", folderUid);
    url.searchParams.set("imapUid", imapUid);
    url.searchParams.set("address", address);
    url.searchParams.set("charset", charset);
    url.searchParams.set("encoding", encoding);
    url.searchParams.set("mime", mime);

    return url;
}

function getPartDownloadUrl(folderUid, imapUid, part) {
    const filename = part.fileName || "";
    const partUrl = getPartUrl(folderUid, imapUid, part);
    partUrl.searchParams.set("filename", filename);
    return partUrl.href.replace(document.baseURI, "");
}

function getPartPreviewUrl(folderUid, imapUid, part) {
    return getPartUrl(folderUid, imapUid, part).href.replace(document.baseURI, "");
}

function createCid() {
    return "<" + UUIDGenerator.generate() + "@bluemind.net>";
}

export {
    CID_DATA_ATTRIBUTE,
    createCid,
    EmailExtractor,
    EmailValidator,
    Flag,
    getPartDownloadUrl,
    getPartPreviewUrl,
    InlineImageHelper,
    mailText2Html,
    MimeType,
    PartsBuilder,
    USED_QUOTA_PERCENTAGE_WARNING,
    WEBSERVER_HANDLER_BASE_URL
};
