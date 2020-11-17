import UUIDGenerator from "@bluemind/uuid";

import { computePreviewOrDownloadUrl, WEBSERVER_HANDLER_BASE_URL } from "./index";

const DATA_ATTRIBUTE_FOR_IMAP_ADDRESS = "data-bm-imap-address";

export default {
    async insertAsUrl(contentsWithCids, imageParts, folderUid, imapUid) {
        const getNewSrcFn = part => computePreviewOrDownloadUrl(folderUid, imapUid, part);
        return insertInHtml(contentsWithCids, imageParts, false, getNewSrcFn);
    },

    async insertAsBase64(contentsWithCids, imageParts, contentByAddress) {
        const getNewSrcFn = part => contentByAddress[part.address];
        return insertInHtml(contentsWithCids, imageParts, true, getNewSrcFn);
    },

    insertCid(html, previousInlineImages) {
        const newContentByCid = {},
            newParts = [];

        // FIXME: use fragment and innerHtml instead of DOMParser.parseFromString
        const imageTags = new DOMParser().parseFromString(html, "text/html").querySelectorAll("img[src]");
        imageTags.forEach(img => {
            const dataSrc = img.src;
            if (dataSrc.startsWith("data:image")) {
                // new inlines
                const cid = UUIDGenerator.generate() + "@bluemind.net";

                // modify tag.src instead (once base64 data are retrieved)
                html = html.replace(dataSrc, "cid:" + cid);

                const extractDataRegex = /data:image(.*)base64,/g;
                const metadatas = dataSrc.match(extractDataRegex)[0];
                const data = dataSrc.replace(metadatas, "");
                // FIXME: use a part create function
                newParts.push({
                    address: null,
                    mime: metadatas.substring(5, metadatas.length - 8),
                    dispositionType: "INLINE",
                    encoding: "base64",
                    contentId: cid
                });
                newContentByCid[cid] = convertData(data);
            } else if (dataSrc.startsWith(WEBSERVER_HANDLER_BASE_URL)) {
                // already fetched inlines
                const imapAddress = img.attributes.getNamedItem(DATA_ATTRIBUTE_FOR_IMAP_ADDRESS).nodeValue;
                const cid = previousInlineImages.find(part => part.address === imapAddress).contentId;
                html = html.replace(dataSrc, "cid:" + cid.substring(1, cid.length - 1));
            }
        });
        return { html, newParts, newContentByCid };
    }
};

function convertData(b64Data) {
    const byteCharacters = atob(b64Data);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    return new Uint8Array(byteNumbers);
}

// function convertToBase64(blob) {
//     const reader = new FileReader();
//     reader.readAsDataURL(blob);
//     return new Promise(resolve => {
//         reader.onloadend = () => {
//             resolve(reader.result);
//         };
//     });
// }

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
function insertInHtml(htmlWithCids = [], imageParts = [], removeImapAddress, getNewSrcFn) {
    const result = { imageInlined: [], contentsWithImageInserted: [] };
    const inlineReferenceRegex = /<img[^>]+?src\s*=\s*['"]cid:([^'"]*)['"][^>]*?>{1}?/gim;

    for (const html of htmlWithCids) {
        let inlineReferences,
            modifiedHtml = html;
        while ((inlineReferences = inlineReferenceRegex.exec(htmlWithCids)) !== null) {
            const cid = inlineReferences[1];
            const replaceRegex = new RegExp("(<img[^>]+?src\\s*=\\s*['\"])cid:" + cid + "(['\"][^>]*?>{1}?)", "gmi");

            const imagePart = imageParts.find(
                part => part.contentId && part.contentId.toUpperCase() === "<" + cid.toUpperCase() + ">"
            );

            if (imagePart) {
                const newSrc = getNewSrcFn(imagePart);
                modifiedHtml = modifiedHtml.replace(replaceRegex, "$1" + newSrc + "$2");
                result.imageInlined.push(imagePart);
            }
        }
        result.contentsWithImageInserted.push(modifiedHtml);
    }
    return result;
}
