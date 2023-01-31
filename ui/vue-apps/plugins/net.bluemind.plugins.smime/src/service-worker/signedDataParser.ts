import parse from "emailjs-mime-parser";

export default function (eml: string): { toDigest: string; pkcs7Part: ArrayBuffer } {
    const rootNode = parse(eml);
    // FIXME: use MimeType
    if (rootNode.contentType.value !== "multipart/signed") {
        throw "SignedMimeParser expects root part to be a multipart/signed";
    }
    const toDigest = rootNode.childNodes[0].raw;
    const pkcs7Part = rootNode.childNodes[1].content;
    return { toDigest, pkcs7Part };
}
