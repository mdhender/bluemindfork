import parse from "emailjs-mime-parser";
import { MimeType } from "@bluemind/email";

export default function (eml: string): { toDigest: string; pkcs7Part: ArrayBuffer } {
    const rootNode = parse(eml);
    if (rootNode.contentType.value !== MimeType.MULTIPART_SIGNED) {
        throw "SignedMimeParser expects root part to be a " + MimeType.MULTIPART_SIGNED;
    }
    const toDigest = rootNode.childNodes[0].raw;
    const pkcs7Part = rootNode.childNodes[1].content;
    return { toDigest, pkcs7Part };
}
