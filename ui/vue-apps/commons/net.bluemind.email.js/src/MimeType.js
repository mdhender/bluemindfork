const TEXT_PLAIN = "text/plain";
const TEXT = "text/";
const TEXT_HTML = "text/html";
const TEXT_CALENDAR = "text/calendar";
const MULTIPART_RELATED = "multipart/related";
const MULTIPART_ALTERNATIVE = "multipart/alternative";
const MULTIPART_MIXED = "multipart/mixed";
const MULTIPART = "multipart/";

const ICS = "application/ics";
const VCARD = "text/vcard";
const IMAGE = "image/";
const AUDIO = "audio/";
const VIDEO = "video/";
const FONT = "font/";
const MESSAGE = "message/";
const PDF = "application/pdf";
const MS_WORD = "application/msword";
const MS_WORD_XML = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
const MS_EXCEL = "application/vnd.ms-excel";
const MS_EXCEL_XML = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
const MS_POWERPOINT = "application/vnd.ms-powerpoint";
const MS_POWERPOINT_XML = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
const XML = "application/xml";
const ZIP = "application/zip";
const RAR = "application/x-rar-compressed";
const ZIP_7 = "application/x-7z-compressed";
const BZIP = "application/x-bzip";
const BZIP2 = "application/x-bzip2";
const TAR = "application/x-tar";
const TGZ = "application/x-gzip";
const SHELL = "application/x-sh";
const CSHELL = "application/x-csh";
const BINARY = "application/octet-stream";
const CSV = "text/csv";
const CSS = "text/css";
const JAVA_JAR = "application/java-archive";
const JAVASCRIPT = "application/javascript";
const JSON = "application/json";
const XHTML = "application/xhtml+xml";
const TYPESCRIPT = "application/typescript";
const OPEN_DOCUMENT_TEXT = "application/vnd.oasis.opendocument.text";
const OPEN_DOCUMENT_CALC = "application/vnd.oasis.opendocument.spreadsheet";
const OPEN_DOCUMENT_PRESENTATION = "application/vnd.oasis.opendocument.presentation";
const SVG = "image/svg+xml";

export default {
    AUDIO,
    TEXT,
    TEXT_PLAIN,
    TEXT_HTML,
    TEXT_CALENDAR,
    MULTIPART_RELATED,
    MULTIPART_ALTERNATIVE,
    MULTIPART_MIXED,
    IMAGE,
    ICS,
    PDF,
    VCARD,
    VIDEO,
    equals,
    typeEquals,
    isRelated,
    isAlternative,
    isMixed,
    isText,
    isHtml,
    isCalendar,
    isImage,
    isMultipart,
    matchingIcon,
    previewAvailable,
    getFromFilename
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

function matchingIcon(mimeType) {
    if (mimeType.startsWith(IMAGE)) {
        return "file-type-image";
    } else if (mimeType.startsWith(AUDIO)) {
        return "file-type-audio";
    } else if (equals(mimeType, PDF)) {
        return "file-type-pdf";
    } else if (mimeType.startsWith(VIDEO)) {
        return "file-type-video";
    } else if (
        equals(mimeType, ZIP) ||
        equals(mimeType, TAR) ||
        equals(mimeType, BZIP) ||
        equals(mimeType, BZIP2) ||
        equals(mimeType, RAR) ||
        equals(mimeType, ZIP_7) ||
        equals(mimeType, TGZ)
    ) {
        return "file-type-compressed";
    } else if (
        equals(mimeType, XML) ||
        equals(mimeType, JSON) ||
        equals(mimeType, CSV) ||
        equals(mimeType, TEXT_PLAIN) ||
        mimeType.startsWith(MESSAGE)
    ) {
        return "file-type-data";
    } else if (equals(mimeType, MS_EXCEL) || equals(mimeType, MS_EXCEL_XML) || equals(mimeType, OPEN_DOCUMENT_CALC)) {
        return "file-type-excel";
    } else if (
        equals(mimeType, MS_POWERPOINT) ||
        equals(mimeType, MS_POWERPOINT_XML) ||
        equals(mimeType, OPEN_DOCUMENT_PRESENTATION)
    ) {
        return "file-type-presentation";
    } else if (equals(mimeType, OPEN_DOCUMENT_TEXT) || equals(mimeType, MS_WORD) || equals(mimeType, MS_WORD_XML)) {
        return "file-type-word";
    } else if (mimeType.startsWith(FONT) || equals(mimeType, BINARY)) {
        return "file-type-system";
    } else if (
        equals(mimeType, JAVASCRIPT) ||
        equals(mimeType, JAVA_JAR) ||
        equals(mimeType, TYPESCRIPT) ||
        equals(mimeType, CSS) ||
        equals(mimeType, SHELL) ||
        equals(mimeType, CSHELL) ||
        equals(mimeType, XHTML) ||
        equals(mimeType, TEXT_HTML)
    ) {
        return "file-type-code";
    } else if (equals(mimeType, ICS) || equals(mimeType, TEXT_CALENDAR)) {
        return "file-type-ics";
    } else if (equals(mimeType, VCARD)) {
        return "file-type-vcard";
    } else {
        return "file-type-unknown";
    }
}

function getFromFilename(name) {
    if (name.toLowerCase().endsWith(".pdf")) {
        return PDF;
    }
}

/* 
    At the moment, preview is available only for images.
    Svg preview has been removed since we use webserver URL instead of blob to make preview, it seems browsers dont accept to display SVG in this case
 */
function previewAvailable(mimeType) {
    return mimeType.startsWith(IMAGE) && !equals(mimeType, SVG);
}
