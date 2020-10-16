// @see https://www.npmjs.com/package/xss
import xss from "xss";

// since we are writing the email content in an iframe, we need to add some tags to the whitelist
// some other tags like 'resourcetemplate' are very specific to Bluemind and need to be kept
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
    "data-bm-reply-separator",
    "data-bm-forward-separator",
    "data-bm-imap-address"
];

const ALLOWED_LINK_PROTOCOLS = ["http", "https"];

export default function (html, useInIframe = false) {
    if (!useInIframe) {
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
    return xss.onTagAttr(tag, name, value);
}

function customSafeAttrValue(tag, name, value) {
    // allow blob images
    if (/^img$/i.test(tag) && /^src$/i.test(name) && /^blob:https?:\/\//i.test(value)) {
        return value;
    }
    if (/^img$/i.test(tag) && /^src$/i.test(name) && /^data:image\//i.test(value)) {
        return value;
    }
    return xss.safeAttrValue(tag, name, value);
}

function hasAllowedProtocol(url) {
    return ALLOWED_LINK_PROTOCOLS.map(p => new RegExp("^" + p + "://", "i").test(url)).reduce((a, b) => a || b);
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

function getStyleRules(doc) {
    let styleRules = "";
    const styleTags = doc.styleSheets;
    for (let tag of styleTags) {
        for (let rule of tag.cssRules) {
            rule.selectorText = computeNewSelector(rule.selectorText);
            styleRules += "\n" + rule.cssText;
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
