import { Verb } from "@bluemind/core.container.api";
import { MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";

const TodoListRight = {
    HAS_NO_RIGHTS: 1,
    CAN_READ_MY_TODO_LIST: 2,
    CAN_EDIT_MY_TODO_LIST: 3,
    CAN_MANAGE_SHARES: 4
};

const HANDLED_VERBS = [Verb.All, Verb.Manage, Verb.Write, Verb.Read];

// do not lose Access Controls with other verbs than HANDLED_VERBS
let otherAcl = [];

export default {
    matchingIcon: () => "list",
    matchingFileTypeIcon: () => "file-type-ics",
    allowedFileTypes: () => MimeType.TEXT_CALENDAR || MimeType.ICS || MimeType.TEXT_PLAIN,
    importFileRequest: async (containerUid, file, uploadCanceller) => {
        const encoded = await file.text().then(res => JSON.stringify(res));
        return inject("VTodoPersistence", containerUid).importIcs(encoded, uploadCanceller);
    },
    defaultUserRight: TodoListRight.CAN_EDIT_MY_TODO_LIST,
    defaultDomainRight: TodoListRight.HAS_NO_RIGHTS,
    maxRight: TodoListRight.CAN_MANAGE_SHARES,
    readRight: TodoListRight.CAN_READ_MY_TODO_LIST,
    getOptions: i18n => [
        {
            text: i18n.t("preferences.has_no_rights"),
            value: TodoListRight.HAS_NO_RIGHTS
        },
        {
            text: i18n.t("preferences.tasks.can_read_my_todolist"),
            value: TodoListRight.CAN_READ_MY_TODO_LIST
        },
        {
            text: i18n.t("preferences.tasks.can_edit_my_todolist"),
            value: TodoListRight.CAN_EDIT_MY_TODO_LIST
        },
        {
            text: i18n.t("preferences.tasks.can_edit_my_todolist_and_manage_shares"),
            value: TodoListRight.CAN_MANAGE_SHARES
        }
    ],
    async loadRights(container) {
        const allAcl = await inject("ContainerManagementPersistence", container.uid).getAccessControlList();
        const aclReducer = (res, ac) => {
            HANDLED_VERBS.includes(ac.verb) ? res[0].push(ac) : res[1].push(ac);
            return res;
        };
        const [acl, other] = allAcl.reduce(aclReducer, [[], []]);
        otherAcl = other;

        const { domain: domainUid, userId } = inject("UserSession");
        const domain = aclToRight(domainUid, acl, this.defaultDomainRight);

        const userUids = new Set(
            acl.flatMap(({ subject }) =>
                subject !== domainUid && subject !== userId && subject !== container.owner ? subject : []
            )
        );
        const users = {};
        userUids.forEach(userUid => {
            users[userUid] = aclToRight(userUid, acl, this.defaultUserRight);
        });

        return { users, domain };
    },
    saveRights(rightBySubject, container) {
        return inject("ContainerManagementPersistence", container.uid).setAccessControlList(
            rightsToAcl(rightBySubject)
        );
    }
};

function aclToRight(subjectUid, acl, defaultRight) {
    const extractVerbs = acl => acl.flatMap(({ subject, verb }) => (subject === subjectUid ? verb : []));
    const verbs = extractVerbs(acl);
    return verbsToRight(verbs, defaultRight);
}

function verbsToRight(verbs, defaultRight) {
    if (verbs.includes(Verb.All) || verbs.includes(Verb.Manage)) {
        return TodoListRight.CAN_MANAGE_SHARES;
    }
    if (verbs.includes(Verb.Write)) {
        return TodoListRight.CAN_EDIT_MY_TODO_LIST;
    }
    if (verbs.includes(Verb.Read)) {
        return TodoListRight.CAN_READ_MY_TODO_LIST;
    }
    return defaultRight;
}

function rightsToAcl(rightBySubject) {
    const acl = [];

    Object.entries(rightBySubject).forEach(([subject, right]) => {
        switch (right) {
            case TodoListRight.CAN_READ_MY_TODO_LIST:
                acl.push({ verb: Verb.Read, subject });
                break;
            case TodoListRight.CAN_EDIT_MY_TODO_LIST:
                acl.push({ verb: Verb.Write, subject });
                break;
            case TodoListRight.CAN_MANAGE_SHARES:
                acl.push({ verb: Verb.Write, subject });
                acl.push({ verb: Verb.Manage, subject });
                break;
            default:
                break;
        }
    });

    return acl.concat(otherAcl);
}
