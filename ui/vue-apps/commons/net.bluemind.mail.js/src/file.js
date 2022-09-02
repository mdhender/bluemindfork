import { isViewable } from "./part";

export const FileStatus = {
    ONLY_LOCAL: "ONLY_LOCAL",
    NOT_LOADED: "NOT-LOADED",
    UPLOADED: "UPLOADED",
    ERROR: "ERROR",
    INVALID: "INVALID"
};

const LARGE_FILE_SIZE = 100 * 1024 * 1024;
const VERY_LARGE_FILE_SIZE = 500 * 1024 * 1024;

function isUploading({ status }) {
    return ![FileStatus.UPLOADED, FileStatus.ERROR, FileStatus.INVALID].includes(status);
}

function isLarge({ size }) {
    return size > LARGE_FILE_SIZE;
}

function isAllowedToPreview(file) {
    return isViewable(file) && file.size && !isUploading(file) && !isLarge(file);
}

function hasRemoteContent({ url }) {
    return url && url.startsWith("http");
}

export default {
    FileStatus,
    hasRemoteContent,
    isAllowedToPreview,
    isLarge,
    isUploading,
    LARGE_FILE_SIZE,
    VERY_LARGE_FILE_SIZE
};
