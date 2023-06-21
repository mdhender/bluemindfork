// @see https://www.npmjs.com/package/xss
import xss from "xss";

import { CID_DATA_ATTRIBUTE, WEBSERVER_HANDLER_BASE_URL } from "@bluemind/email";

// since we are writing the email content in an iframe, we need to add some tags to the whitelist
// some other tags like 'resourcetemplate' are very specific to BlueMind and need to be kept
const ADDITIONAL_ALLOWED_TAGS = {
    html: [],
    body: [],
    head: [],
    style: [],
    button: [],
    table: [...xss.whiteList.table, "cellspacing", "cellpadding"],
    resourcetemplate: ["id"]
};

// in order to have fancy emails we keep some attributes
const ADDITIONAL_ALLOWED_ATTRIBUTES_FOR_ANY_TAG = [
    "class",
    "type",
    "style",
    "id",
    "height",
    "width",
    "border",
    "bgcolor",
    "leftmargin",
    "topmargin",
    "marginwidth",
    "marginheight",
    CID_DATA_ATTRIBUTE,
    "data-bm-forward-separator",
    "data-bm-reply-separator",
    "data-bm-signature",
    "data-bm-color-variables"
];
const WINDOWS_FILEPATH_PROTOCOL = "[a-z]";
const ALLOWED_LINK_PROTOCOLS = ["http", "https", "mailto", "tel", "sip", "file", WINDOWS_FILEPATH_PROTOCOL];
const LINK_REGEX = new RegExp(`^(${ALLOWED_LINK_PROTOCOLS.join("|")}):(.*)`, "i");

export default function (html, avoidStyleInvading) {
    if (avoidStyleInvading) {
        html = preventStyleInvading(html);
    }

    const customWhiteList = {
        ...xss.whiteList,
        ...ADDITIONAL_ALLOWED_TAGS
    };
    const xssFilter = new xss.FilterXSS({
        css: false,
        stripIgnoreTag: true,
        stripIgnoreTagBody: ["script", "title"],
        whiteList: customWhiteList,
        onIgnoreTagAttr: customOnIgnoreTagAttr,
        onTagAttr: customOnTagAttr,
        safeAttrValue: customSafeAttrValue
    });
    html = xssFilter.process(html);

    return html;
}

function customOnIgnoreTagAttr(tag, name, value) {
    if (ADDITIONAL_ALLOWED_ATTRIBUTES_FOR_ANY_TAG.includes(name)) {
        return name + '="' + value + '"';
    }
    return xss.onIgnoreTagAttr(tag, name, value);
}

function customOnTagAttr(tag, name, value) {
    // disable links having a forbidden protocol
    if (/^a$/i.test(tag) && /^href$/i.test(name) && !hasAllowedProtocol(value)) {
        return "";
    }
    // remove viewport height based styles
    if (/^style$/i.test(name)) {
        const newValue = value.replace(/\s*(min-)?(max-)?height\s*:\s*[0-9]+vh\s*;/g, "");
        return name + '="' + newValue + '"';
    }
    return xss.onTagAttr(tag, name, value);
}

function customSafeAttrValue(tag, name, value) {
    // allow blob images
    if (/^img$/i.test(tag) && /^src$/i.test(name) && /^blob:https?:\/\//i.test(value)) {
        return value;
    }
    // allow image if src matches our webserver handler url (see PartContentUrlHandler.java)
    if (/^img$/i.test(tag) && /^src$/i.test(name) && value.startsWith(WEBSERVER_HANDLER_BASE_URL)) {
        return value;
    }

    if (/^img$/i.test(tag) && /^src$/i.test(name) && /^data:image\//i.test(value)) {
        return value;
    }

    // rectify link protocol case
    if (/^a$/i.test(tag) && /^href$/i.test(name) && hasAllowedProtocol(value)) {
        const linkInfo = value.match(LINK_REGEX);
        const protocol = linkInfo[1];
        const path = linkInfo[2];
        const rectified = protocol.toLowerCase();
        value = `${rectified}:${path}`;

        // SIP protocol specific
        if (rectified === "sip") {
            return safeSipPath(tag, name, path);
        }

        // open bar for FILE protocol (asked by P.Baudracco)
        if (rectified === "file") {
            return value;
        }

        // open bar for WINDOWS path (asked by P.Baudracco)
        if (new RegExp(WINDOWS_FILEPATH_PROTOCOL, "i").test(rectified)) {
            return value;
        }
    }

    return xss.safeAttrValue(tag, name, value);
}

/** Since xss.safeAttrValue rejects SIP protocol, instead we use HTTP protocol to validate the value of the path.  */
function safeSipPath(tag, name, path) {
    return xss.safeAttrValue(tag, name, `https://${path}`) ? `sip:${path}` : "";
}

function hasAllowedProtocol(url) {
    return ALLOWED_LINK_PROTOCOLS.map(p => new RegExp(`^${p}:`, "i").test(url)).reduce((a, b) => a || b);
}

/**
 * WARNING: this is an internal const, it's exported just for testing purpose
 */
export const WRAPPER_ID = "bm-composer-content-wrapper";

/**
 * WARNING: this is an internal method, it's exported just for testing purpose
 */
export function preventStyleInvading(html) {
    const tmpDoc = new DOMParser().parseFromString(html, "text/html");

    const styleRules = getStyleRules(tmpDoc);

    const rootDiv = tmpDoc.createElement("div");
    rootDiv.id = WRAPPER_ID;
    rootDiv.innerHTML = tmpDoc.body.innerHTML;

    const rootDivStyleTags = rootDiv.getElementsByTagName("style");
    while (rootDivStyleTags.length > 0) {
        rootDivStyleTags.item(0).remove();
    }

    const styleNode = document.createElement("style");
    styleNode.innerHTML = styleRules;
    rootDiv.appendChild(styleNode);

    return rootDiv.outerHTML;
}

/**
 * WARNING: this is an internal method, it's exported just for testing purpose
 */
export function getStyleRules(doc) {
    let styleRules = "";
    const styleTags = doc.styleSheets;
    for (let tag of styleTags) {
        for (let rule of tag.cssRules) {
            if (rule.selectorText) {
                rule.selectorText = computeNewSelector(rule.selectorText);
                styleRules += "\n" + rule.cssText;
            }
        }
    }
    return styleRules;
}

/**
 * WARNING: this is an internal method, it's exported just for testing purpose
 */
export function computeNewSelector(selectorText) {
    let selectors = selectorText.split(",");
    return selectors
        .map(selector => selector.trim().replace(/^([\s>+~]*(html|body)(\.[^\s>]*)?[\s]*)*/g, ""))
        .map(selector => "#" + WRAPPER_ID + " " + selector)
        .join(",");
}
