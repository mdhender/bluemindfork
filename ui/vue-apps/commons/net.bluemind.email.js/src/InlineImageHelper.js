import { computePreviewOrDownloadUrl, WEBSERVER_HANDLER_BASE_URL } from "./index";

const CID_DATA_ATTRIBUTE = "data-bm-cid";

export default {
    async insertAsUrl(contentsWithCids, imageParts, folderUid, imapUid) {
        const getNewSrcFn = part => computePreviewOrDownloadUrl(folderUid, imapUid, part);
        return insertInHtml(contentsWithCids, imageParts, getNewSrcFn);
    },

    async insertAsBase64(contentsWithCids, imageParts, contentByAddress) {
        const getNewSrcFn = part => contentByAddress[part.address];
        return insertInHtml(contentsWithCids, imageParts, getNewSrcFn);
    },

    insertCid(html, inlineImagesSaved) {
        const result = {
            htmlWithCids: html,
            newParts: [],
            newContentByCid: {},
            alreadySaved: []
        };

        const imageTags = new DOMParser()
            .parseFromString(result.htmlWithCids, "text/html")
            .querySelectorAll("img[src]");
        imageTags.forEach(img => {
            const cidDataAttribute = img.attributes[CID_DATA_ATTRIBUTE];
            if (cidDataAttribute) {
                const cid = cidDataAttribute.nodeValue;
                const cidSrc = "cid:" + cid.slice(1, -1);
                if (img.src.startsWith("data:image")) {
                    // check if cid part has already been created (needed when same cid is referenced multiple times)
                    if (!result.newParts.find(part => part.contentId === cid)) {
                        if (!isImgAlreadySaved(cid, inlineImagesSaved)) {
                            const extractDataRegex = /data:image(.*)base64,/g;
                            const metadatas = img.src.match(extractDataRegex)[0];
                            const data = img.src.replace(metadatas, "");
                            result.newParts.push({
                                address: null,
                                mime: getMimeType(metadatas),
                                dispositionType: "INLINE",
                                encoding: "base64",
                                contentId: cid
                            });
                            result.newContentByCid[cid] = convertData(data);
                        } else {
                            result.alreadySaved.push(inlineImagesSaved.find(part => part.contentId === cid));
                        }
                    }

                    result.htmlWithCids = result.htmlWithCids.replace(img.attributes["src"].nodeValue, cidSrc); // dont use img.src because it would fail to replace b64 image including line break
                } else if (img.attributes.src.nodeValue.startsWith(WEBSERVER_HANDLER_BASE_URL)) {
                    const encoded = encodeHtmlEntities(img.attributes.src.nodeValue);
                    result.alreadySaved.push(inlineImagesSaved.find(part => part.contentId === cid));
                    result.htmlWithCids = result.htmlWithCids.replace(encoded, cidSrc);
                }
            }
        });
        return result;
    }
};

function isImgAlreadySaved(cid, inlineImagesSaved) {
    return inlineImagesSaved.findIndex(part => part.contentId === cid) !== -1;
}

function encodeHtmlEntities(str) {
    let tmp = document.createElement("p");
    tmp.innerHTML = str;
    return tmp.innerHTML;
}

function convertData(b64Data) {
    const sanitized = b64Data.replace(/[^aA-zZ+0-9/=]/, "");
    const byteCharacters = atob(sanitized);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    return new Uint8Array(byteNumbers);
}

/**
 * Replace the CID references found in htmlWithCids by the corresponding images.
 *
 * @param htmlWithCids html with CID references
 * @param imageParts parts with base64 images and a CID identifier
 * @param getNewSrcFn execute this function with a part as param to compute new src attribute value
 * @returns an object containing
 *          imageInlined: cids for which we found at least one reference in contents
 *          contentsWithImageInserted: modified contents
 */
function insertInHtml(htmlWithCids = [], imageParts = [], getNewSrcFn) {
    const result = { imageInlined: [], contentsWithImageInserted: [] };
    const inlineReferenceRegex = /<img[^>]+?src\s*=\s*['"]cid:([^'"]*)['"][^>]*?>{1}?/gim;

    for (const html of htmlWithCids) {
        let inlineReferences,
            modifiedHtml = html;
        while ((inlineReferences = inlineReferenceRegex.exec(modifiedHtml)) !== null) {
            const cid = inlineReferences[1];
            const replaceRegex = new RegExp("(<img[^>]+?src\\s*=\\s*['\"])cid:" + cid + "(['\"][^>]*?>{1}?)", "gmi");
            const imagePart = imageParts.find(
                part =>
                    part.contentId &&
                    (part.contentId.toUpperCase() === "<" + cid.toUpperCase() + ">" ||
                        part.contentId.toUpperCase() === cid.toUpperCase())
            );
            if (imagePart) {
                let newSrc = getNewSrcFn(imagePart);
                newSrc += '" ' + CID_DATA_ATTRIBUTE + '="' + imagePart.contentId;
                modifiedHtml = modifiedHtml.replace(replaceRegex, "$1" + newSrc + "$2");
                result.imageInlined.push(imagePart);
            }
        }
        result.contentsWithImageInserted.push(modifiedHtml);
    }
    return result;
}

function getMimeType(metadatas) {
    const withoutData = metadatas.replace("data:", "");
    return withoutData.substring(0, withoutData.indexOf(";"));
}
