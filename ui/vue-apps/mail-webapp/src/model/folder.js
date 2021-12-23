import { inject } from "@bluemind/inject";
import { MailboxType } from "./mailbox";
import injector from "@bluemind/inject";

export function create(key, name, parent, mailbox) {
    const defaultFolder = isDefault(!parent, name, mailbox);
    const translatedName = defaultFolder ? translate(name) : name;
    const folderPath = path(mailbox, translatedName, parent);
    return {
        key: key,
        remoteRef: {
            uid: null,
            internalId: null
        },
        mailboxRef: {
            uid: mailbox.remoteRef.uid,
            key: mailbox.key
        },
        parent: parent ? parent.key : null,
        name: translatedName,
        imapName: name,
        path: folderPath,
        writable: mailbox.writable,
        allowConversations: allowConversations(folderPath, mailbox),
        allowSubfolder: allowSubfolder(mailbox.writable, !parent, name, mailbox),
        default: defaultFolder,
        expanded: false,
        unread: 0
    };
}

export function rename(folder, name) {
    const path = folder.path.replace(new RegExp(folder.name + "$"), name);
    return { ...folder, name, path };
}

export function move(folder, parent, mailbox) {
    return { ...folder, path: path(mailbox, folder.name, parent), parent: parent.key };
}

export const DEFAULT_FOLDERS = {
    INBOX: "INBOX",
    SENT: "Sent",
    DRAFTS: "Drafts",
    TRASH: "Trash",
    JUNK: "Junk",
    TEMPLATES: "Templates",
    OUTBOX: "Outbox"
};

const DEFAULT_FOLDER_AS_ARRAY = Object.values(DEFAULT_FOLDERS);

function path(mailbox, name, parent) {
    if (parent) {
        return [parent.path, name].filter(Boolean).join("/");
    } else if (mailbox.type === MailboxType.MAILSHARE) {
        return mailbox.root;
    } else {
        return name;
    }
}
export function isDefault(isRoot, name, mailbox) {
    if (mailbox.type === MailboxType.USER) {
        return isRoot && !!DEFAULT_FOLDERS[name.toUpperCase()];
    } else if (mailbox.type === MailboxType.MAILSHARE) {
        return isRoot || !!DEFAULT_FOLDERS[name.toUpperCase()];
    }
}

export function isMailshareRoot(folder, mailbox) {
    return mailbox.type === MailboxType.MAILSHARE && !folder.parent;
}

export function allowSubfolder(writable, isRoot, name, mailbox) {
    let allowed = !isDefault(isRoot, name, mailbox);
    allowed |= DEFAULT_FOLDERS.INBOX.toUpperCase() === name.toUpperCase();
    allowed |= mailbox.type === MailboxType.MAILSHARE && isRoot;
    return Boolean(writable && allowed);
}

export function allowConversations(path, mailbox) {
    if (mailbox.type === MailboxType.MAILSHARE) {
        return false;
    }

    const rootFolderName = path.split("/")[0];
    return ![
        DEFAULT_FOLDERS.SENT,
        DEFAULT_FOLDERS.DRAFTS,
        DEFAULT_FOLDERS.TEMPLATES,
        DEFAULT_FOLDERS.TRASH,
        DEFAULT_FOLDERS.JUNK,
        DEFAULT_FOLDERS.OUTBOX
    ].includes(rootFolderName);
}

export function translatePath(path) {
    const splitPath = path.split("/");
    const rootFolder = splitPath[0];
    const defaultFolder = DEFAULT_FOLDERS[rootFolder.toUpperCase()];

    if (defaultFolder) {
        splitPath.splice(0, 1, translateDefaultName(defaultFolder));
    }

    return splitPath.join("/");
}

function translateDefaultName(name) {
    const vueI18n = injector.getProvider("i18n").get();
    return vueI18n.t("common.folder." + name.toLowerCase());
}

export function normalize(folderPath, getFolderByPath) {
    let fixedPath = "";
    folderPath.split("/").forEach(pathPart => {
        fixedPath += pathPart;
        fixedPath = fixExistingFolderName(fixedPath, getFolderByPath);
        fixedPath += "/";
    });
    return removeTrailingSlashes(fixedPath);
}

function fixExistingFolderName(folderName, getFolderByPath) {
    let folder = getFolderByPath(folderName);
    if (!folder) {
        const defaultFolder = Object.values(DEFAULT_FOLDERS).find(
            f => translateDefaultName(f).toUpperCase() === folderName.toUpperCase()
        );
        folder = defaultFolder ? { path: defaultFolder } : null;
    }
    return folder ? folder.path : folderName;
}

function removeTrailingSlashes(path) {
    return path.replace(/\/+$/, "");
}

const FOLDER_PATH_MAX_LENGTH = 250;

/** @return true or an explanation about why it's not valid */
export function isNameValid(name, path, getFolderByPath) {
    const vueI18n = injector.getProvider("i18n").get();

    path = removeTrailingSlashes(path);

    if (path.length > FOLDER_PATH_MAX_LENGTH) {
        return vueI18n.t("mail.actions.folder.invalid.too_long");
    }

    const checkValidity = isFolderNameValid(name.toLowerCase());
    if (checkValidity !== true) {
        return vueI18n.t("common.invalid.character", {
            character: checkValidity
        });
    }

    const normalizedPath = normalize(path, getFolderByPath);
    if (getFolderByPath(normalizedPath)) {
        return vueI18n.t("mail.actions.folder.invalid.already_exist");
    }

    if (!ascendantsAllowSubfolder(normalizedPath, getFolderByPath)) {
        return vueI18n.t("mail.actions.folder.subfolder.forbidden");
    }

    return true;
}

function ascendantsAllowSubfolder(normalizedPath, getFolderByPath) {
    const parentPath = normalizedPath.substring(0, normalizedPath.lastIndexOf("/"));
    const parentFolder = getFolderByPath(parentPath);
    return (
        !parentPath ||
        (ascendantsAllowSubfolder(parentPath, getFolderByPath) && (!parentFolder || parentFolder.allowSubfolder))
    );
}

const FORBIDDEN_FOLDER_CHARACTERS = '@%*"`;^<>{}|\\';

/** @return invalid character if name is invalid */
function isFolderNameValid(name) {
    for (let i = 0; i < name.length; i++) {
        if (FORBIDDEN_FOLDER_CHARACTERS.includes(name.charAt(i))) {
            return name.charAt(i);
        }
    }
    return true;
}

function translate(name) {
    if (DEFAULT_FOLDERS[name.toUpperCase()]) {
        return inject("i18n").t("common.folder." + name.toLowerCase()) || name;
    }
    return name;
}

export function compare(f1, f2) {
    if (f1.mailboxRef.key !== f2.mailboxRef.key) {
        return f1.mailboxRef.key > f2.mailboxRef.key ? 1 : -1;
    }
    let w1 = DEFAULT_FOLDER_AS_ARRAY.indexOf(f1.imapName) + 1 || DEFAULT_FOLDER_AS_ARRAY.length;
    let w2 = DEFAULT_FOLDER_AS_ARRAY.indexOf(f2.imapName) + 1 || DEFAULT_FOLDER_AS_ARRAY.length;
    return w1 - w2 || f1.path.localeCompare(f2.path);
}

export function generateKey(folderUid) {
    return folderUid;
}

export function isDraftFolder(path) {
    const rootFolderName = path.split("/")[0];
    return DEFAULT_FOLDERS.DRAFTS === rootFolderName;
}

export function match(folder, pattern) {
    pattern = pattern.toLowerCase();
    if (folder.path.toLowerCase().includes(pattern)) {
        return true;
    }
    return false;
}
