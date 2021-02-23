// FIXME: must call part creator with some additional properties
export function create(address, charset, fileName, encoding, mime, size, isUploaded) {
    let progress, status;
    if (!isUploaded) {
        progress = { loaded: 0, total: 100 };
        status = AttachmentStatus.NOT_LOADED;
    } else {
        progress = { loaded: 100, total: 100 };
        status = AttachmentStatus.UPLOADED;
    }

    return {
        address,
        charset,
        fileName,
        encoding,
        mime,
        size,
        dispositionType: "ATTACHMENT",
        headers: getAttachmentHeaders(fileName, size),
        progress,
        status
    };
}

export const AttachmentStatus = {
    NOT_LOADED: "NOT-LOADED",
    UPLOADED: "UPLOADED",
    ERROR: "ERROR"
};

export function getAttachmentHeaders(name, size) {
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
