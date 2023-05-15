import { MessageBody } from "@bluemind/backend.mail.api";

const TEXT_PLAIN = "text/plain";
const TEXT = "text/";
const TEXT_HTML = "text/html";
const TEXT_CALENDAR = "text/calendar";
const MULTIPART_RELATED = "multipart/related";
const MULTIPART_REPORT = "multipart/report";
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
const MESSAGE_DELIVERY_STATUS = "message/delivery-status";
const MESSAGE_DISPOSITION_NOTIFICATION = "message/disposition-notification";
const TEXT_RFC822_HEADERS = "text/rfc822-headers";
const EML = "message/rfc822";
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
const GIF = "image/gif";
const JPEG = "image/jpeg";
const PNG = "image/png";
const WAV = "audio/wav";
const MP3 = "audio/mp3";
const MOV = "video/mov";
const MP4 = "video/mp4";
const AVI = "image/avi";

// SMIME
const PKCS_8 = "application/pkcs8";
const PKCS_12 = "application/x-pkcs12";
const CRYPTO_CERT = "application/pkix-cert";
const X509_CERT = "application/x-x509-ca-cert";
const PEM_FILE = "application/x-pem-file";
const PKCS_7 = "application/pkcs7-mime";
const PKCS_7_SIGNED_DATA = "application/pkcs7-signature";
const MULTIPART_SIGNED = "multipart/signed";

const MP4_SUFFIXES = ["mp4"];
const MOV_SUFFIXES = ["mov"];
const AVI_SUFFIXES = ["avi"];
const GIF_SUFFIXES = ["gif"];
const JPEG_SUFFIXES = ["jpg", "jpeg"];
const PNG_SUFFIXES = ["png"];
const SVG_SUFFIXES = ["svg"];
const MP3_SUFFIXES = ["mp3"];
const WAV_SUFFIXES = ["wav"];
const PDF_SUFFIXES = ["pdf"];
const ZIP_SUFFIXES = ["zip"];
const ICS_SUFFIXES = ["ics"];
const VCARD_SUFFIXES = ["vcf"];
const FONT_SUFFIXES = ["otf", "ttf", "fnt"];
const MS_WORD_SUFFIXES = ["doc", "dot"];
const MS_WORD_XML_SUFFIXES = ["docx", "dotx"];
const MS_EXCEL_SUFFIXES = ["xls", "xlt", "xla"];
const MS_EXCEL_XML_SUFFIXES = ["xlsx", "xltx"];
const MS_POWERPOINT_SUFFIXES = ["ppt", "pot", "pps", "ppa"];
const MS_POWERPOINT_XML_SUFFIXES = ["pptx", "potx", "ppsx"];
const XML_SUFFIXES = ["xml"];
const RAR_SUFFIXES = ["rar"];
const ZIP_7_SUFFIXES = ["7z"];
const BZIP_SUFFIXES = ["bzip", "bzip2"];
const TAR_SUFFIXES = ["tar"];
const TGZ_SUFFIXES = ["gzip"];
const SHELL_SUFFIXES = ["sh"];
const CSHELL_SUFFIXES = ["csh"];
const BINARY_SUFFIXES = ["bin", "dat", "exe"];
const CSV_SUFFIXES = ["csv"];
const TEXT_PLAIN_SUFFIXES = ["txt"];
const TEXT_HTML_SUFFIXES = ["html"];
const JAVA_JAR_SUFFIXES = ["jar"];
const TYPESCRIPT_SUFFIXES = ["js"];
const JAVASCRIPT_SUFFIXES = ["js"];
const JSON_SUFFIXES = ["json"];
const XHTML_SUFFIXES = ["xhtml"];
const OPEN_DOCUMENT_TEXT_SUFFIXES = ["odt"];
const OPEN_DOCUMENT_CALC_SUFFIXES = ["odc"];
const OPEN_DOCUMENT_PRESENTATION_SUFFIXES = ["odp"];
const EML_SUFFIXES = ["eml"];
const PKCS_12_SUFFIXES = ["p12", "pfx"];

export default {
    AUDIO,
    CRYPTO_CERT,
    EML,
    ICS,
    IMAGE,
    MESSAGE,
    MESSAGE_DELIVERY_STATUS,
    MESSAGE_DISPOSITION_NOTIFICATION,
    MULTIPART_ALTERNATIVE,
    MULTIPART_MIXED,
    MULTIPART_RELATED,
    MULTIPART_REPORT,
    MULTIPART_SIGNED,
    PDF,
    PEM_FILE,
    PKCS_7,
    PKCS_7_SIGNED_DATA,
    PKCS_8,
    PKCS_12,
    PKCS_12_SUFFIXES,
    TEXT,
    TEXT_PLAIN,
    TEXT_HTML,
    TEXT_CALENDAR,
    TEXT_RFC822_HEADERS,
    VCARD,
    VIDEO,
    X509_CERT,
    equals,
    typeEquals,
    isRelated,
    isAlternative,
    isMixed,
    isText,
    isHtml,
    isCalendar,
    isAudio,
    isImage,
    isVideo,
    isMultipart,
    isPkcs7,
    matchingIcon,
    previewAvailable,
    getFromFilename
};

/** Compare MIME type and subtype. */
function equals(mimeType1: string, mimeType2: string) {
    return mimeType1.toLowerCase() === mimeType2.toLowerCase();
}

/** Compare only MIME type. */
function typeEquals(mimeType1: string, mimeType2: string) {
    return (
        mimeType1.substring(0, mimeType1.indexOf("/")).toLowerCase() ===
        mimeType2.substring(0, mimeType2.indexOf("/")).toLowerCase()
    );
}

function isRelated(part: MessageBody.Part) {
    return equals(part.mime!, MULTIPART_RELATED);
}

function isAlternative(part: MessageBody.Part) {
    return equals(part.mime!, MULTIPART_ALTERNATIVE);
}

function isMixed(part: MessageBody.Part) {
    return equals(part.mime!, MULTIPART_MIXED);
}

function isText(part: MessageBody.Part) {
    return equals(part.mime!, TEXT_PLAIN);
}

function isHtml(part: MessageBody.Part) {
    return equals(part.mime!, TEXT_HTML);
}

function isCalendar(part: MessageBody.Part) {
    return equals(part.mime!, TEXT_CALENDAR);
}

function isAudio(part: MessageBody.Part) {
    return part.mime!.startsWith(AUDIO);
}
function isImage(part: MessageBody.Part) {
    return part.mime!.startsWith(IMAGE);
}
function isVideo(part: MessageBody.Part) {
    return part.mime!.startsWith(VIDEO);
}

function isMultipart(part: MessageBody.Part) {
    return part.mime!.startsWith(MULTIPART);
}

function isPkcs7(part: MessageBody.Part) {
    return [PKCS_7, "application/x-pkcs7-mime"].includes(part.mime!);
}

function matchingIcon(mimeType: string) {
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
        equals(mimeType, MESSAGE_DISPOSITION_NOTIFICATION) ||
        equals(mimeType, MESSAGE_DELIVERY_STATUS) ||
        equals(mimeType, TEXT_RFC822_HEADERS)
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
    } else if (equals(mimeType, EML)) {
        return "file-type-message";
    } else {
        return "file-type-unknown";
    }
}

function getFromFilename(name: string) {
    const suffix = name.split(".").pop()?.toLowerCase();
    if (!suffix) {
        return "";
    }
    if (MP4_SUFFIXES.includes(suffix)) {
        return MP4;
    } else if (MOV_SUFFIXES.includes(suffix)) {
        return MOV;
    } else if (AVI_SUFFIXES.includes(suffix)) {
        return AVI;
    } else if (GIF_SUFFIXES.includes(suffix)) {
        return GIF;
    } else if (JPEG_SUFFIXES.includes(suffix)) {
        return JPEG;
    } else if (PNG_SUFFIXES.includes(suffix)) {
        return PNG;
    } else if (SVG_SUFFIXES.includes(suffix)) {
        return SVG;
    } else if (MP3_SUFFIXES.includes(suffix)) {
        return MP3;
    } else if (WAV_SUFFIXES.includes(suffix)) {
        return WAV;
    } else if (PDF_SUFFIXES.includes(suffix)) {
        return PDF;
    } else if (ZIP_SUFFIXES.includes(suffix)) {
        return ZIP;
    } else if (ICS_SUFFIXES.includes(suffix)) {
        return ICS;
    } else if (VCARD_SUFFIXES.includes(suffix)) {
        return VCARD;
    } else if (FONT_SUFFIXES.includes(suffix)) {
        return FONT;
    } else if (MS_WORD_SUFFIXES.includes(suffix)) {
        return MS_WORD;
    } else if (MS_WORD_XML_SUFFIXES.includes(suffix)) {
        return MS_WORD_XML;
    } else if (MS_EXCEL_SUFFIXES.includes(suffix)) {
        return MS_EXCEL;
    } else if (MS_EXCEL_XML_SUFFIXES.includes(suffix)) {
        return MS_EXCEL_XML;
    } else if (MS_POWERPOINT_SUFFIXES.includes(suffix)) {
        return MS_POWERPOINT;
    } else if (MS_POWERPOINT_XML_SUFFIXES.includes(suffix)) {
        return MS_POWERPOINT_XML;
    } else if (XML_SUFFIXES.includes(suffix)) {
        return XML;
    } else if (RAR_SUFFIXES.includes(suffix)) {
        return RAR;
    } else if (ZIP_7_SUFFIXES.includes(suffix)) {
        return ZIP_7;
    } else if (BZIP_SUFFIXES.includes(suffix)) {
        return BZIP;
    } else if (TAR_SUFFIXES.includes(suffix)) {
        return TAR;
    } else if (TGZ_SUFFIXES.includes(suffix)) {
        return TGZ;
    } else if (SHELL_SUFFIXES.includes(suffix)) {
        return SHELL;
    } else if (CSHELL_SUFFIXES.includes(suffix)) {
        return CSHELL;
    } else if (BINARY_SUFFIXES.includes(suffix)) {
        return BINARY;
    } else if (CSV_SUFFIXES.includes(suffix)) {
        return CSV;
    } else if (TEXT_PLAIN_SUFFIXES.includes(suffix)) {
        return TEXT_PLAIN;
    } else if (TEXT_HTML_SUFFIXES.includes(suffix)) {
        return TEXT_HTML;
    } else if (JAVA_JAR_SUFFIXES.includes(suffix)) {
        return JAVA_JAR;
    } else if (JAVASCRIPT_SUFFIXES.includes(suffix)) {
        return JAVASCRIPT;
    } else if (TYPESCRIPT_SUFFIXES.includes(suffix)) {
        return TYPESCRIPT;
    } else if (JSON_SUFFIXES.includes(suffix)) {
        return JSON;
    } else if (XHTML_SUFFIXES.includes(suffix)) {
        return XHTML;
    } else if (OPEN_DOCUMENT_TEXT_SUFFIXES.includes(suffix)) {
        return OPEN_DOCUMENT_TEXT;
    } else if (OPEN_DOCUMENT_CALC_SUFFIXES.includes(suffix)) {
        return OPEN_DOCUMENT_CALC;
    } else if (OPEN_DOCUMENT_PRESENTATION_SUFFIXES.includes(suffix)) {
        return OPEN_DOCUMENT_PRESENTATION;
    } else if (EML_SUFFIXES.includes(suffix)) {
        return EML;
    } else if (PKCS_12_SUFFIXES.includes(suffix)) {
        return PKCS_12;
    } else {
        return "";
    }
}
/* 
    At the moment, preview is available only for images.
    Svg preview has been removed since we use webserver URL instead of blob to make preview, it seems browsers dont accept to display SVG in this case
 */
function previewAvailable(mimeType: string) {
    return mimeType.startsWith(IMAGE) && !equals(mimeType, SVG);
}
