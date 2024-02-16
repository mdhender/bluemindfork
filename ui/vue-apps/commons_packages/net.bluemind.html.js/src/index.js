export { blockRemoteImages, hasRemoteImages, unblockRemoteImages } from "./blockRemoteImages";
export { removeXDisclaimer } from "./removeXDisclaimer";
export { default as html2text } from "./Html2Text";
export { default as text2html } from "./text2html";
export { default as preventStyleInvading } from "./preventStyleInvading";
export { default as removeDuplicatedIds } from "./removeDuplicatedIds";
export { default as sanitizeHtml } from "./sanitizeHtml";
export { default as EmptyTransformer } from "./transformers/EmptyTransformer";
export { default as createDocumentFragment } from "./createDocumentFragment";

export function containsHtml(str) {
    return /<[a-z][\s\S]*>/i.test(str);
}
