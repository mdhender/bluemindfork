import { getPartDownloadUrl, MimeType } from "@bluemind/email";
import i18n from "@bluemind/i18n";
import file from "./file";

const { FileStatus } = file;

export function create(part, status) {
    const progress = status === FileStatus.NOT_LOADED ? { loaded: 0, total: 100 } : { loaded: 100, total: 100 };
    const filename = part.fileName ? part.fileName : i18n.t("mail.attachment.untitled", { mimeType: part.mime });

    let mime = part.mime;
    const mimeFromExtension = mime === "application/octet-stream" && getTypeFromExtension(filename);
    if (mimeFromExtension) {
        mime = mimeFromExtension;
    }

    const headers = part.headers ? [...getAttachmentHeaders(part), ...part.headers] : getAttachmentHeaders(part);

    let attachment = {
        dispositionType: "ATTACHMENT",
        progress,
        status,
        ...part,
        filename,
        mime,
        headers
    };
    return attachment;
}

function getTypeFromExtension(filename) {
    return MimeType.getFromFilename(filename);
}

export function getAttachmentHeaders({ fileName, size }) {
    return [
        {
            name: "Content-Disposition",
            values: ["attachment;filename=" + fileName + ";size=" + size]
        },
        {
            name: "Content-Transfer-Encoding",
            values: ["base64"]
        }
    ];
}

/**
 * @return true if we consider the given part should be attached,
 *         i.e.: not shown in the message body, false otherwise
 */
export function isAttachment(part) {
    return (part.dispositionType && part.dispositionType !== "INLINE") || part.mime === "application/octet-stream";
}

const AttachmentAdaptor = {
    extractFiles(attachments, message) {
        const adaptedAttachements = [];
        const adaptedFiles = [];
        attachments.forEach(att => {
            const key = att.address + ":" + message.key;
            adaptedAttachements.push({ fileKey: key, address: att.address });
            adaptedFiles.push(this.createFileFromPart(att, key, message));
        });
        return {
            attachments: adaptedAttachements,
            files: adaptedFiles
        };
    },
    createFileFromPart(part, key, message) {
        const progress =
            part.status === FileStatus.NOT_LOADED ? { loaded: 0, total: 100 } : { loaded: 100, total: 100 };
        const name = part.fileName || i18n.t("mail.attachment.untitled", { mimeType: part.mime });
        const mime = part.mime || "application/octet-stream";
        const url = message ? getPartDownloadUrl(message.folderRef.uid, message.remoteRef.imapUid, part) : null;
        const file = {
            address: part.address,
            key,
            charset: "us-ascii",
            encoding: "base64",
            name,
            mime,
            size: part.size,
            progress,
            status: part.status,
            url,
            extra: {},
            ...part
        };
        return file;
    }
};

export default {
    create,
    AttachmentAdaptor,
    getAttachmentHeaders,
    isAttachment
};
