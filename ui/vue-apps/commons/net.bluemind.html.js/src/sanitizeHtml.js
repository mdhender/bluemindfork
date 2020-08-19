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
    "marginheight"
];

const ALLOWED_LINK_PROTOCOLS = ["http", "https"];

export default function (html) {
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
