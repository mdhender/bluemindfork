const TEXT_PLAIN = "text/plain";
const TEXT_HTML = "text/html";
const TEXT_CALENDAR = "text/calendar";
const MULTIPART_RELATED = "multipart/related";
const MULTIPART_ALTERNATIVE = "multipart/alternative";
const MULTIPART_MIXED = "multipart/mixed";
const MULTIPART = "multipart/";
const IMAGE = "image/";

export default {
    TEXT_PLAIN,
    TEXT_HTML,
    TEXT_CALENDAR,
    MULTIPART_RELATED,
    MULTIPART_ALTERNATIVE,
    MULTIPART_MIXED,
    IMAGE,
    equals,
    typeEquals,
    isRelated,
    isAlternative,
    isMixed,
    isText,
    isHtml,
    isCalendar,
    isImage,
    isMultipart
};

/** Compare MIME type and subtype. */
function equals(mimeType1, mimeType2) {
    return mimeType1.toLowerCase() === mimeType2.toLowerCase();
}

/** Compare only MIME type. */
function typeEquals(mimeType1, mimeType2) {
    return (
        mimeType1.substring(0, mimeType1.indexOf("/")).toLowerCase() ===
        mimeType2.substring(0, mimeType2.indexOf("/")).toLowerCase()
    );
}

function isRelated(part) {
    return equals(part.mime, MULTIPART_RELATED);
}

function isAlternative(part) {
    return equals(part.mime, MULTIPART_ALTERNATIVE);
}

function isMixed(part) {
    return equals(part.mime, MULTIPART_MIXED);
}

function isText(part) {
    return equals(part.mime, TEXT_PLAIN);
}

function isHtml(part) {
    return equals(part.mime, TEXT_HTML);
}

function isCalendar(part) {
    return equals(part.mime, TEXT_CALENDAR);
}

function isImage(part) {
    return part.mime.startsWith(IMAGE);
}

function isMultipart(part) {
    return part.mime.startsWith(MULTIPART);
}
