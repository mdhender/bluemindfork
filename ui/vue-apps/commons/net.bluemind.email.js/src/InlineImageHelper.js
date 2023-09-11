import { getPartPreviewUrl } from "./index";

export const CID_DATA_ATTRIBUTE = "data-bm-cid";

let objectURLs = {};

export default {
    insertAsUrl(contentsWithCids, imageParts, folderUid, imapUid) {
        const getNewSrcFn = part => getPartPreviewUrl(folderUid, imapUid, part);
        return insertInHtml(contentsWithCids, imageParts, getNewSrcFn);
    },
    insertAsLocalUrl(contentsWithCids, imageParts, imagePartsData, uid) {
        const getNewSrcFn = part => {
            const objectURL = URL.createObjectURL(new Blob([imagePartsData[part.address]]));
            if (!objectURLs[uid]) {
                objectURLs[uid] = [];
            }
            objectURLs[uid].push(objectURL);
            return objectURL;
        };
        return insertInHtml(contentsWithCids, imageParts, getNewSrcFn);
    },
    cleanLocalImages(uid) {
        objectURLs[uid]?.forEach(objectURL => URL.revokeObjectURL(objectURL));
        delete objectURLs[uid];
    },
    insertAsBase64(contentsWithCids, imageParts, contentByAddress) {
        const getNewSrcFn = part => contentByAddress[part.address];
        return insertInHtml(contentsWithCids, imageParts, getNewSrcFn);
    },
    cids(html) {
        return getCids(html);
    }
};

const CID_REFERENCE_REGEXP = /<img[^>]+?src\s*=\s*['"]cid:([^'"]*)['"][^>]*?>/gim;
function getCids(html) {
    return [...html.matchAll(CID_REFERENCE_REGEXP)].map(match => match[1].toUpperCase());
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

    for (const html of htmlWithCids) {
        let modifiedHtml = html;
        for (let cid of getCids(html)) {
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
