import { inject } from "@bluemind/inject";
import { MailboxType } from "./mailbox";
import injector from "@bluemind/inject";

export function create(key, name, parent, mailbox) {
    const defaultFolder = isDefault(!parent, name, mailbox);
    const translatedName = defaultFolder ? translate(name) : name;
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
        path: path(mailbox, translatedName, parent),
        writable: mailbox.writable,
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

export const DEFAULT_FOLDERS = {
    INBOX: "INBOX",
    SENT: "Sent",
    DRAFTS: "Drafts",
    TRASH: "Trash",
    JUNK: "Junk",
    OUTBOX: "Outbox"
};

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
    return isRoot && (mailbox.type !== MailboxType.USER || !!DEFAULT_FOLDERS[name.toUpperCase()]);
}

export function isMailshareRoot(folder, mailbox) {
    return mailbox.type === MailboxType.MAILSHARE && !folder.parent;
}

export function allowSubfolder(writable, isRoot, name, mailbox) {
    return (
        writable && (!isDefault(isRoot, name, mailbox) || DEFAULT_FOLDERS.INBOX.toUpperCase() === name.toUpperCase())
    );
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

export function normalize(folderPath, foldersByUpperCasePath) {
    let fixedPath = "";
    folderPath.split("/").forEach(pathPart => {
        fixedPath += pathPart;
        fixedPath = fixExistingFolderName(fixedPath, foldersByUpperCasePath);
        fixedPath += "/";
    });
    return removeTrailingSlashes(fixedPath);
}

function fixExistingFolderName(folderName, foldersByUpperCasePath) {
    let folder = foldersByUpperCasePath[folderName.toUpperCase()];
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
export function isNameValid(name, path, foldersByUpperCasePath) {
    const vueI18n = injector.getProvider("i18n").get();

    path = removeTrailingSlashes(path);

    if (path.length > FOLDER_PATH_MAX_LENGTH) {
        return vueI18n.t("mail.actions.folder.invalid.too_long");
    }

    const checkValidity = isFolderNameValid(name.toLowerCase());
    if (checkValidity !== true) {
        return vueI18n.t("mail.actions.folder.invalid.character", {
            character: checkValidity
        });
    }

    const normalizedPath = normalize(path, foldersByUpperCasePath);
    if (foldersByUpperCasePath[normalizedPath.toUpperCase()]) {
        return vueI18n.t("mail.actions.folder.invalid.already_exist");
    }

    if (!ascendantsAllowSubfolder(normalizedPath, foldersByUpperCasePath)) {
        return vueI18n.t("mail.actions.folder.subfolder.forbidden");
    }

    return true;
}

function ascendantsAllowSubfolder(normalizedPath, foldersByUpperCasePath) {
    const parentPath = normalizedPath.substring(0, normalizedPath.lastIndexOf("/"));
    const parentFolder = foldersByUpperCasePath[parentPath.toUpperCase()];
    return (
        !parentPath ||
        (ascendantsAllowSubfolder(parentPath, foldersByUpperCasePath) && (!parentFolder || parentFolder.allowSubfolder))
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
    const f1Weight = Object.values(DEFAULT_FOLDERS).indexOf(f1.imapName);
    const f2Weight = Object.values(DEFAULT_FOLDERS).indexOf(f2.imapName);
    if (f1Weight >= 0 && f2Weight >= 0) {
        return f1Weight - f2Weight;
    } else if (f1Weight >= 0 && f2Weight < 0) {
        return -1;
    } else if (f1Weight < 0 && f2Weight >= 0) {
        return 1;
    } else {
        return f1.imapName.localeCompare(f2.imapName);
    }
}

export function generateKey(folderUid) {
    return folderUid;
}
