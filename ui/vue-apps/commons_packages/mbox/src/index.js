export function extractFolderUid(containerUid) {
    return containerUid.replace("mbox_records_", "");
}

export default { extractFolderUid };
