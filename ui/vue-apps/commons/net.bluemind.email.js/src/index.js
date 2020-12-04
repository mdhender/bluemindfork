import UUIDGenerator from "@bluemind/uuid";

import EmailExtractor from "./EmailExtractor";
import EmailValidator from "./EmailValidator";
import Flag from "./Flag";
import InlineImageHelper from "./InlineImageHelper";
import mailText2Html from "./mailText2Html";
import MimeType from "./MimeType";
import PartsBuilder from "./PartsBuilder";

export { EmailExtractor, EmailValidator, Flag, InlineImageHelper, mailText2Html, MimeType, PartsBuilder };

export const WEBSERVER_HANDLER_BASE_URL = "part/url/";

export function computePreviewOrDownloadUrl(folderUid, imapUid, part) {
    const filename = part.fileName ? "&filename=" + part.fileName : "";

    const baseUrl = document.baseURI;

    const url = new URL(
        WEBSERVER_HANDLER_BASE_URL +
            "?folderUid=" +
            folderUid +
            "&imapUid=" +
            imapUid +
            "&address=" +
            part.address +
            "&encoding=" +
            part.encoding +
            "&mime=" +
            part.mime +
            "&charset=" +
            part.charset +
            filename,
        baseUrl
    );
    return url.href.replace(baseUrl, "");
}

export function createCid() {
    return "<" + UUIDGenerator.generate() + "@bluemind.net>";
}
