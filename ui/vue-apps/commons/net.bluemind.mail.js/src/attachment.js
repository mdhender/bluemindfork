import { getPartDownloadUrl, MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import injector from "@bluemind/inject";
import UUIDGenerator from "@bluemind/uuid";
import file from "./file";

const { FileStatus } = file;

export function create(part, status) {
    const progress = status === FileStatus.NOT_LOADED ? { loaded: 0, total: 100 } : { loaded: 100, total: 100 };
    if (!part.fileName) {
        part.fileName = inject("i18n").t("mail.attachment.untitled", { mimeType: part.mime });
    }
    if (part.mime === "application/octet-stream" && part.fileName && getTypeFromExtension(part.fileName)) {
        part.mime = getTypeFromExtension(part.fileName);
    }
    const headers = part.headers ? [...getAttachmentHeaders(part), ...part.headers] : getAttachmentHeaders(part);

    let attachment = {
        dispositionType: "ATTACHMENT",
        progress,
        status,
        ...part,
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
            const fileKey = UUIDGenerator.generate();
            adaptedAttachements.push({ fileKey, address: att.address });
            adaptedFiles.push(this.createFileFromPart(att, fileKey, message));
        });
        return {
            attachments: adaptedAttachements,
            files: adaptedFiles
        };
    },
    createFileFromPart(part, key, message) {
        const progress =
            part.status === FileStatus.NOT_LOADED ? { loaded: 0, total: 100 } : { loaded: 100, total: 100 };
        const name =
            part.fileName || injector.getProvider("i18n").get().t("mail.attachment.untitled", { mimeType: part.mime });
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
