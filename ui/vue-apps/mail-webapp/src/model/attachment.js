export function create(address, charset, name, encoding, mime, size, isUploaded) {
    let headers, progress, status;
    if (!isUploaded) {
        progress = { loaded: 0, total: 100 };
        headers = uploadAttachmentHeaders(name, size);
        status = AttachmentStatus.NOT_LOADED;
    } else {
        progress = { loaded: 100, total: 100 };
        headers = [];
        status = AttachmentStatus.LOADED;
    }
    return {
        address,
        charset,
        filename: name,
        encoding,
        mime,
        size,
        headers,
        progress,
        status,
        contentUrl: null
    };
}

export const AttachmentStatus = {
    NOT_LOADED: "NOT-LOADED",
    LOADED: "LOADED",
    ERROR: "ERROR"
};

export function uploadAttachmentHeaders(name, size) {
    return [
        {
            name: "Content-Disposition",
            values: ["attachment;filename=" + name + ";size=" + size]
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
    return part.dispositionType && part.dispositionType === "ATTACHMENT";
}
