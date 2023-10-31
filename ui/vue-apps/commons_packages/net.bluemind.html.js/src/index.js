import { blockRemoteImages, hasRemoteImages, unblockRemoteImages } from "./blockRemoteImages";
import html2text from "./Html2Text";
import text2html from "./text2html";
import sanitizeHtml from "./sanitizeHtml";
import EmptyTransformer from "./transformers/EmptyTransformer";
import createDocumentFragment from "./createDocumentFragment";
export {
    blockRemoteImages,
    containsHtml,
    createDocumentFragment,
    EmptyTransformer,
    hasRemoteImages,
    html2text,
    sanitizeHtml,
    text2html,
    unblockRemoteImages
};

function containsHtml(str) {
    return /<[a-z][\s\S]*>/i.test(str);
}
