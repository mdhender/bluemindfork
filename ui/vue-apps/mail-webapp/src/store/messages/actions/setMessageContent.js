import cloneDeep from "lodash.clonedeep";
import { inject } from "@bluemind/inject";
import { partUtils } from "@bluemind/mail";
import { html2text } from "@bluemind/html-utils";
import { base64ToArrayBuffer } from "@bluemind/arraybuffer";
import { PartsBuilder, MimeType, CID_DATA_ATTRIBUTE } from "@bluemind/email";
import { SET_MESSAGE_PREVIEW } from "~/mutations";
import { UPDATE_MESSAGE_STRUCTURE } from "~/actions";

const { sanitizeTextPartForCyrus } = partUtils;

export default async function setMessageContent({ dispatch, commit }, { message, content }) {
    commit(SET_MESSAGE_PREVIEW, { key: message.key, preview: html2text(content) });
    const structure = await buildBodyStructureFromContent(message, content);
    return dispatch(UPDATE_MESSAGE_STRUCTURE, { key: message.key, structure });
}

async function buildBodyStructureFromContent(message, html) {
    const htmlContent = sanitizeTextPartForCyrus(html);
    const textContent = sanitizeTextPartForCyrus(html2text(htmlContent));
    return buildContentPart(htmlContent, textContent, message);
}

async function buildContentPart(htmlContent, textContent, message) {
    const alternativePart = await buildAlternativePart(htmlContent, textContent, message);
    const isMixed = message.structure.mime === MimeType.MULTIPART_MIXED;

    if (isMixed) {
        const oldStructure = cloneDeep(message.structure);
        const attachmentParts = oldStructure.children.filter(({ dispositionType }) => dispositionType === "ATTACHMENT");
        return PartsBuilder.createMixedPart([alternativePart, ...attachmentParts]);
    }
    return alternativePart;
}

async function buildAlternativePart(htmlContent, textContent, message) {
    const service = inject("MailboxItemsPersistence", message.folderRef.uid);
    const alternativePart = getMainAlternativePart(message.structure);
    const htmlRelatedPart = await buildHtmlRelatedPart(service, alternativePart, htmlContent);
    const textPlainPart = await buildTextPlainPart(service, textContent);
    const partsToPreserve = getPartsToPreserve(alternativePart);
    return PartsBuilder.createAlternativePart(textPlainPart, htmlRelatedPart, ...partsToPreserve);
}

function getPartsToPreserve(alternativePart) {
    return alternativePart.children.filter(
        part => part.mime && ![MimeType.TEXT_HTML, MimeType.TEXT_PLAIN, MimeType.MULTIPART_RELATED].includes(part.mime)
    );
}

function getMainAlternativePart(structure) {
    const isAlternative = ({ mime }) => mime === MimeType.MULTIPART_ALTERNATIVE;
    const alternativePart = isAlternative(structure) ? structure : structure.children.find(isAlternative);
    return cloneDeep(alternativePart);
}

async function buildTextPlainPart(service, textContent) {
    const address = await service.uploadPart(textContent);
    return PartsBuilder.createTextPart(address);
}

async function buildHtmlRelatedPart(service, alternativePart, htmlContent) {
    const { imageNodesByCid, htmlWithCids } = extractBase64Images(htmlContent);
    const oldRelatedPart = alternativePart.children.find(({ mime }) => mime === MimeType.MULTIPART_RELATED);
    const relatedPart = await buildRelatedPart(service, oldRelatedPart, imageNodesByCid, htmlWithCids);
    return sanitizeRelated(relatedPart);
}

async function buildNewHtmlPart(service, htmlWithCids) {
    const address = await service.uploadPart(htmlWithCids);
    return PartsBuilder.createHtmlPart(address);
}

async function buildRelatedPart(service, oldRelatedPart, imageNodesByCid, htmlWithCids) {
    const htmlPart = await buildNewHtmlPart(service, htmlWithCids);
    const imageParts = removeObsoleteImages(oldRelatedPart, imageNodesByCid);
    const newImageParts = await uploadNewImages(service, oldRelatedPart, imageNodesByCid);
    return PartsBuilder.createRelatedPart([htmlPart, ...imageParts, ...newImageParts]);
}

async function uploadNewImages(service, relatedPart, imageNodesByCid) {
    const children = relatedPart?.children || [];
    const existingCids = children.map(({ contentId }) => contentId);

    const newImageParts = [];
    for (const cid in imageNodesByCid) {
        if (!existingCids.includes(cid) && imageNodesByCid[cid]?.data) {
            const { data, mime, size } = imageNodesByCid[cid];
            const address = await service.uploadPart(data);
            const part = PartsBuilder.createInlinePart({ mime, size, address, cid });
            newImageParts.push(part);
        }
    }
    return newImageParts;
}

function sanitizeRelated(relatedPart) {
    if (relatedPart.children.length === 1) {
        return relatedPart.children.pop();
    }
    return relatedPart;
}

function removeObsoleteImages(relatedPart, imageNodesByCid) {
    const inlineParts = relatedPart?.children || [];
    const cids = Object.keys(imageNodesByCid);
    return inlineParts.filter(part => cids.includes(part.contentId));
}
function extractBase64Images(htmlContent) {
    let htmlWithCids = htmlContent;
    const imageNodes = Array.from(
        new DOMParser().parseFromString(htmlContent, "text/html").querySelectorAll("img[src]")
    );
    const imageNodesByCid = imageNodes
        .filter(node => node.attributes.src.nodeValue?.startsWith("data:image") && extractCid(node))
        .reduce((nodesByCid, node) => {
            const cid = extractCid(node);
            if (!nodesByCid[cid]) {
                const cidSrc = `cid:${cid.slice(1, -1)}`;
                const { data, metadata } = extractDataFromImg(node);
                if (data && metadata) {
                    nodesByCid[cid] = {
                        data,
                        mime: getMimeType(metadata),
                        size: data.byteLength
                    };
                    htmlWithCids = htmlWithCids.replaceAll(encodeHtmlEntities(node.attributes.src.nodeValue), cidSrc);
                }
            }
            return nodesByCid;
        }, {});

    return { imageNodesByCid, htmlWithCids };
}

function encodeHtmlEntities(str) {
    let tmp = document.createElement("p");
    tmp.innerHTML = str;
    return tmp.innerHTML;
}

function extractCid(imageNode) {
    return imageNode.attributes[CID_DATA_ATTRIBUTE]?.nodeValue;
}

function extractDataFromImg(imageNode) {
    const extractDataRegex = /data:image(.*)base64,/g;
    const metadata = imageNode.src.match(extractDataRegex)[0];
    const data = imageNode.src.replace(metadata, "");
    return { data: convertData(data), metadata };
}

function convertData(b64Data = "") {
    const sanitized = sanitizeB64(b64Data);
    return base64ToArrayBuffer(sanitized);
}

function sanitizeB64(base64) {
    return base64.replaceAll("%0A", "").replace(/[^aA-zZ+0-9/=]/g, "");
}

function getMimeType(metadatas) {
    const withoutData = metadatas?.replace("data:", "");
    return withoutData?.substring(0, withoutData.indexOf(";"));
}
