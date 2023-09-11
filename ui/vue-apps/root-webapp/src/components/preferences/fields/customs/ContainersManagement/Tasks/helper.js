import { Verb } from "@bluemind/core.container.api";
import { MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";

const TodoListAcl = {
    HAS_NO_RIGHTS: 0,
    CAN_READ_MY_TODO_LIST: 1,
    CAN_EDIT_MY_TODO_LIST: 2,
    CAN_MANAGE_SHARES: 3
};

const HANDLED_VERBS = [Verb.All, Verb.Manage, Verb.Write, Verb.Read];

export default {
    matchingIcon: () => "list",
    matchingFileTypeIcon: () => "file-type-ics",
    allowedFileTypes: () => MimeType.TEXT_CALENDAR || MimeType.ICS || MimeType.TEXT_PLAIN,
    importFileRequest: async (containerUid, file, uploadCanceller) => {
        const encoded = await file.text().then(res => JSON.stringify(res));
        return inject("VTodoPersistence", containerUid).importIcs(encoded, uploadCanceller);
    },
    buildDefaultDirEntryAcl: dirEntry => [{ subject: dirEntry.uid, verb: Verb.Read }],
    defaultDomainAcl: [],
    getOptions: i18n => [
        {
            text: i18n.t("preferences.has_no_rights"),
            value: TodoListAcl.HAS_NO_RIGHTS
        },
        {
            text: i18n.t("preferences.tasks.can_read_my_todolist"),
            value: TodoListAcl.CAN_READ_MY_TODO_LIST
        },
        {
            text: i18n.t("preferences.tasks.can_edit_my_todolist"),
            value: TodoListAcl.CAN_EDIT_MY_TODO_LIST
        },
        {
            text: i18n.t("preferences.tasks.can_edit_my_todolist_and_manage_shares"),
            value: TodoListAcl.CAN_MANAGE_SHARES
        }
    ],
    aclToOption: acl => {
        const verbs = acl.map(({ verb }) => verb);

        if (verbs.includes(Verb.All) || verbs.includes(Verb.Manage)) {
            return TodoListAcl.CAN_MANAGE_SHARES;
        }
        if (verbs.includes(Verb.Write)) {
            return TodoListAcl.CAN_EDIT_MY_TODO_LIST;
        }
        if (verbs.includes(Verb.Read)) {
            return TodoListAcl.CAN_READ_MY_TODO_LIST;
        }
        return TodoListAcl.HAS_NO_RIGHTS;
    },
    updateAcl(acl, subject, option) {
        if (this.aclToOption(acl) !== option) {
            const newAcl = acl.flatMap(ac => (!HANDLED_VERBS.includes(ac.verb) ? ac : []));
            switch (option) {
                case TodoListAcl.CAN_READ_MY_TODO_LIST:
                    newAcl.push({ verb: Verb.Read, subject });
                    break;
                case TodoListAcl.CAN_EDIT_MY_TODO_LIST:
                    newAcl.push({ verb: Verb.Write, subject });
                    break;
                case TodoListAcl.CAN_MANAGE_SHARES:
                    newAcl.push({ verb: Verb.Write, subject });
                    newAcl.push({ verb: Verb.Manage, subject });
                    break;
                default:
                    break;
            }
            return newAcl;
        }
    }
};
