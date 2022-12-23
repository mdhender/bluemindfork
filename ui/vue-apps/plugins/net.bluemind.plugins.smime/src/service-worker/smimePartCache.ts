export function getCacheKey(folderUid: string, imapUid: number, address: string) {
    let key;
    if (isImapAddress(address)) {
        key = `${folderUid}/${imapUid}/${address}`;
    } else {
        key = `${folderUid}/${address}`;
    }
    return `/part/${key}`;
}

function isImapAddress(address: string) {
    const regex = /^[0-9.]*$/;
    return address === "TEXT" || address === "HEADER" || regex.test(address);
}
