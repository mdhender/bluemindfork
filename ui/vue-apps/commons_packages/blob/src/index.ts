import { MessageBody } from "@bluemind/backend.mail.api";
import { MimeType } from "@bluemind/email";

export async function convertBlob(blob: Blob, part: MessageBody.Part) {
    return MimeType.isHtml(part) ||
        MimeType.isText(part) ||
        MimeType.MESSAGE_DISPOSITION_NOTIFICATION === part.mime ||
        MimeType.MESSAGE_DELIVERY_STATUS === part.mime
        ? await convertBlobToText(blob, part)
        : await convertBlobToBase64(blob);
}

export function convertBlobToText(blob: Blob, part: MessageBody.Part) {
    return new Promise(resolve => {
        const reader = new FileReader();
        reader.readAsText(blob, part.charset);
        reader.addEventListener("loadend", e => {
            resolve(e.target?.result);
        });
    });
}

export function convertBlobToBase64(blob: Blob) {
    const reader = new FileReader();
    reader.readAsDataURL(blob);
    return new Promise(resolve => {
        reader.onloadend = () => {
            resolve(reader.result);
        };
    });
}
