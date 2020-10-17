import UUIDGenerator from "@bluemind/uuid";

export default {
    /**
     * Replace the CID references found in partsContainingCids by the corresponding images.
     *
     * @param partsContainingCids parts with CID references inside an image markup
     * @param imageParts parts with base64 images and a CID identifier
     */
    insertInlineImages(partsContainingCids = [], imageParts = [], partContentByAddress) {
        const inlineReferenceRegex = /<img[^>]+?src\s*=\s*['"]cid:([^'"]*)['"][^>]*?>{1}?/gim;

        const inlined = [],
            blobsUrl = [];

        partsContainingCids.forEach(part => {
            let inlineReferences;
            while ((inlineReferences = inlineReferenceRegex.exec(partContentByAddress[part.address])) !== null) {
                const cid = inlineReferences[1];
                const replaceRegex = new RegExp(
                    "(<img[^>]+?src\\s*=\\s*['\"])cid:" + cid + "(['\"][^>]*?>{1}?)",
                    "gmi"
                );
                const imagePart = imageParts.find(
                    imgPart =>
                        imgPart.contentId &&
                        imgPart.contentId.toUpperCase() === "<" + inlineReferences[1].toUpperCase() + ">"
                );
                if (imagePart) {
                    // FIXME: mutate state here..
                    const blobUrl = URL.createObjectURL(partContentByAddress[imagePart.address]);
                    blobsUrl.push(blobUrl);

                    partContentByAddress[part.address] = partContentByAddress[part.address].replace(
                        replaceRegex,
                        "$1" + blobUrl + '" data-bm-imap-address="' + imagePart.address + "$2"
                    );
                    inlined.push(imagePart.contentId);
                }
            }
        });
        return { inlined, blobsUrl };
    },

    insertCid(html, inlineImages) {
        const streamByCid = {};

        const imageTags = new DOMParser().parseFromString(html, "text/html").querySelectorAll("img[src]");
        imageTags.forEach(img => {
            const dataSrc = img.src;
            if (dataSrc.startsWith("data:image")) {
                // new inlines
                const cid = UUIDGenerator.generate() + "@bluemind.net";
                html = html.replace(dataSrc, "cid:" + cid);

                const extractDataRegex = /data:image(.*)base64,/g;
                const metadatas = dataSrc.match(extractDataRegex)[0];
                const data = dataSrc.replace(metadatas, "");
                inlineImages.push({
                    address: null,
                    mime: metadatas.substring(5, metadatas.length - 8),
                    dispositionType: "INLINE",
                    encoding: "base64",
                    contentId: cid
                });
                streamByCid[cid] = convertData(data);
            } else if (dataSrc.startsWith("blob:")) {
                // already fetched inlines
                const imapAddress = img.attributes.getNamedItem("data-bm-imap-address").nodeValue;
                const cid = inlineImages.find(part => part.address === imapAddress).contentId;
                html = html.replace(dataSrc, "cid:" + cid.substring(1, cid.length - 1));
            }
        });
        return { html, inlineImages, streamByCid };
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
