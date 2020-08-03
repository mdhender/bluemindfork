/**
 * @param {Array} mailboxes like:
 *    [{ mailboxUid: "mbuid",
 *        type:"mailshare",
 *        root:"Groot",
 *        folders: [{value: { fullName: "m-folder1-blue" }, parentUid: mailshareMailbox}] }]
 */
export function applyFilterThenSliceAndTransform(mailboxes, filterFunction, maxFolders) {
    return mailboxes.reduce((matches, mailbox) => {
        return matches.concat(_filter(mailbox, filterFunction, maxFolders - matches.length));
    }, []);
}

function _filter(mailbox, filter, max) {
    const matches = [];
    for (let i = 0; i < mailbox.folders.length && matches.length < max; i++) {
        const folder = mailbox.folders[i];
        const root = folder.parent !== null ? mailbox.root : "";
        if (filter(folder, Object.assign({}, mailbox, { root }))) {
            const folderItem = toFolderItem(
                folder,
                mailbox.type === "mailshare",
                ((root && root + "/") || "") + folder.path
            );
            matches.push(folderItem);
        }
    }
    return matches;
}

export function toFolderItem(folder, isShared = false, path = undefined) {
    return { folder, isShared, path };
}

export const FolderHelper = { applyFilterThenSliceAndTransform, toFolderItem };
export default FolderHelper;
