import { fileUtils } from "@bluemind/mail";
const { FileStatus } = fileUtils;
export function getFhHeader(headers = []) {
    return (
        headers.find(header => header?.name?.toLowerCase() === "x-bm-disposition") ||
        headers.find(header => header?.name?.toLowerCase() === "x-mozilla-cloud-part")
    );
}

export function getFhInfos({ key, headers }) {
    const header = getFhHeader(headers);

    if (header) {
        const data = extractFileHostingInfos(header);

        if (!data.name) {
            const contentDispoHeader = headers.find(header => header?.name?.toLowerCase() === "content-disposition");
            const contentDispoData = extractFileHostingInfos(contentDispoHeader);
            data.name = contentDispoData.filename;
        }
        if (data.expirationDate && data.expirationDate < Date.now()) {
            data.status = FileStatus.INVALID;
        }
        return { key, ...data };
    }
}
function extractFileHostingInfos(header) {
    if (!header) {
        return {};
    }
    const headerValue = header.values[0];
    return Object.fromEntries(
        headerValue
            .split(";")
            .slice(1)
            .map(s => s.match(/ *([^=]*)=(.*)/).slice(1, 3))
    );
}
