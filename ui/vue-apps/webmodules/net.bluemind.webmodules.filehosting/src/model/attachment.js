import injector from "@bluemind/inject";

export function create(part, status) {
    const progress = status === AttachmentStatus.NOT_LOADED ? { loaded: 0, total: 100 } : { loaded: 100, total: 100 };
    if (!part.fileName) {
        part.fileName = injector.getProvider("i18n").get().t("mail.attachment.untitled", { mimeType: part.mime });
    }
    const headers = part.headers ? [...getAttachmentHeaders(part), ...part.headers] : getAttachmentHeaders(part);

    let attachment = {
        dispositionType: "ATTACHMENT",
        progress,
        status,
        type: "default",
        extra: {},
        ...part,
        headers
    };
    return attachment;
}

export const AttachmentStatus = {
    ONLY_LOCAL: "ONLY_LOCAL",
    NOT_LOADED: "NOT-LOADED",
    UPLOADED: "UPLOADED",
    ERROR: "ERROR"
};

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
    return part.dispositionType && part.dispositionType !== "INLINE";
}
