import injector from "@bluemind/inject";
import { DEFAULT_FOLDERS } from "./DefaultFolders";
import { MailboxType } from "./MailboxAdaptor";

export const FolderAdaptor = {
    fromMailboxFolder(remotefolder, mailbox) {
        return {
            key: remotefolder.uid,
            uid: remotefolder.uid,
            id: remotefolder.internalId,
            mailbox: mailbox.uid,
            parent: remotefolder.value.parentUid,
            name: this.isDefault(!remotefolder.parentUid, remotefolder.value.name, mailbox)
                ? translateDefaults(remotefolder.value.name)
                : remotefolder.value.name,
            imapName: remotefolder.value.name,
            path: computePathFromRemote(remotefolder, mailbox),
            writable: mailbox.writable,
            default: this.isDefault(!remotefolder.parentUid, remotefolder.value.name, mailbox),
            expanded: false,
            unread: 0
        };
    },
    toMailboxFolder(localfolder, mailbox) {
        return {
            uid: localfolder.uid,
            internalId: localfolder.id,
            value: {
                parentUid: localfolder.parent,
                name: localfolder.name,
                fullName: localfolder.path.replace(new RegExp("^" + mailbox.root + "/"), "")
            }
        };
    },

    isMyMailboxDefaultFolder(folder) {
        return !folder.parent && DEFAULT_FOLDERS.includes(folder.imapName);
    },

    isMailshareRoot(folder, mailbox) {
        return mailbox.type === "mailshares" && !folder.parent;
    },

    create(key, name, parent, mailbox) {
        return {
            key: key,
            uid: null,
            id: null,
            mailbox: mailbox.key,
            parent: parent ? parent.key : null,
            name: name,
            imapName: name,
            path: computePath(mailbox, name, parent),
            writable: mailbox.writable,
            default: this.isDefault(!parent, name, mailbox),
            expanded: false,
            unread: 0
        };
    },

    rename(folder, name) {
        const path = folder.path.replace(new RegExp(folder.name + "$"), name);
        return { ...folder, name, path };
    },

    isDefault(isRootFolder, name, mailbox) {
        return isRootFolder && (mailbox.type !== MailboxType.USER || DEFAULT_FOLDERS.includes(name));
    },

    // return true or an explanation about why it's not valid
    isNameValid(name, path, FOLDER_BY_PATH) {
        const vueI18n = injector.getProvider("i18n").get();

        if (path.length > FOLDER_PATH_MAX_LENGTH) {
            return vueI18n.t("mail.actions.folder.invalid.too_long");
        }

        const checkValidity = isFolderNameValid(name.toLowerCase());
        if (checkValidity !== true) {
            return vueI18n.t("mail.actions.folder.invalid.character", {
                character: checkValidity
            });
        }

        if (FOLDER_BY_PATH(path) || DEFAULT_FOLDERS.includes(path)) {
            return vueI18n.t("mail.actions.folder.invalid.already_exist");
        }

        return true;
    }
};

const FOLDER_PATH_MAX_LENGTH = 250;

const FORBIDDEN_FOLDER_CHARACTERS = '/@%*"`;^<>{}|';

/**
 * return invalid character if name is invalid
 */
function isFolderNameValid(name) {
    for (let i = 0; i < name.length; i++) {
        if (FORBIDDEN_FOLDER_CHARACTERS.includes(name.charAt(i))) {
            return name.charAt(i);
        }
    }
    return true;
}

function computePathFromRemote(remotefolder, mailbox) {
    const folderPath = FolderAdaptor.isDefault(!remotefolder.parentUid, remotefolder.value.name, mailbox)
        ? translateDefaults(remotefolder.value.name)
        : remotefolder.value.fullName;

    if (mailbox.type === "mailshares") {
        if (!remotefolder.value.parentUid) {
            return mailbox.root;
        }
        return path(mailbox.root, folderPath);
    }
    return folderPath;
}

function computePath(mailbox, name, parent) {
    if (parent) {
        return path(parent.path, name);
    } else if (mailbox.type === "mailshares") {
        return mailbox.root;
    } else {
        return name;
    }
}

function path() {
    return Array.from(arguments)
        .filter(Boolean)
        .join("/");
}

function translateDefaults(name) {
    if (DEFAULT_FOLDERS.includes(name)) {
        const vueI18n = injector.getProvider("i18n").get();
        return vueI18n.t("common.folder." + name.toLowerCase());
    }
    return name;
}
