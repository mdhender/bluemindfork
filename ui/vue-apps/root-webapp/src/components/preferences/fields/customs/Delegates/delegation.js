import without from "lodash.without";
import { computed, ref, watch } from "vue";
import { Verb } from "@bluemind/core.container.api";
import i18n from "@bluemind/i18n";
import { inject } from "@bluemind/inject";
import store from "@bluemind/store";

const getContainers = () => store.state.preferences.containers;
const getUserId = () => inject("UserSession").userId;

const mailboxUid = computed(() => getContainers().myMailboxContainer.uid);
const calendarUid = computed(
    () => getContainers().myCalendars.find(({ uid }) => uid === `calendar:Default:${getUserId()}`)?.uid
);
const addressBookUid = computed(
    () => getContainers().myAddressbooks.find(({ uid }) => uid === `book:Contacts_${getUserId()}`)?.uid
);
const todoListUid = computed(
    () => getContainers().myTodoLists.find(({ uid }) => uid === `todolist:default_${getUserId()}`)?.uid
);

const fetchAcl = async uid => {
    return await inject("ContainerManagementPersistence", uid).getAccessControlList();
};

export const acls = ref({
    mailbox: { uid: mailboxUid, acl: undefined },
    calendar: { uid: calendarUid, acl: undefined },
    addressBook: { uid: addressBookUid, acl: undefined },
    todoList: { uid: todoListUid, acl: undefined }
});

export const useDelegation = () => {
    watch(mailboxUid, async () => (acls.value.mailbox.acl = await fetchAcl(mailboxUid.value)), { immediate: true });
    watch(calendarUid, async () => (acls.value.calendar.acl = await fetchAcl(calendarUid.value)), { immediate: true });
    watch(addressBookUid, async () => (acls.value.addressBook.acl = await fetchAcl(addressBookUid.value)), {
        immediate: true
    });
    watch(todoListUid, async () => (acls.value.todoList.acl = await fetchAcl(todoListUid.value)), { immediate: true });
};

export const fetchAcls = async () => {
    if (mailboxUid.value) {
        acls.value.mailbox.acl = await fetchAcl(mailboxUid.value);
    }
    if (calendarUid.value) {
        acls.value.calendar.acl = await fetchAcl(calendarUid.value);
    }
    if (addressBookUid.value) {
        acls.value.addressBook.acl = await fetchAcl(addressBookUid.value);
    }
    if (todoListUid.value) {
        acls.value.todoList.acl = await fetchAcl(todoListUid.value);
    }
};

export const delegations = computed(() => {
    return Object.values(acls.value).flatMap(
        ({ uid, acl }) =>
            acl
                ?.filter(({ subject, verb }) => subject !== getUserId() && DELEGATION_VERBS.includes(verb))
                .map(ac => ({ uid, ac })) || []
    );
});

/** Delegates and their rights: { delegatUid1: {containerUid1: [verb1, verb2]} } */
export const delegates = computed(() => {
    const delegates = {};
    delegations.value.forEach(d => {
        if (!delegates[d.ac.subject]) {
            delegates[d.ac.subject] = {};
        }
        if (!delegates[d.ac.subject][d.uid]) {
            delegates[d.ac.subject][d.uid] = [];
        }
        delegates[d.ac.subject][d.uid].push(d.ac.verb);
    });
    return delegates;
});

export const removeDelegate = userUid => {
    const cleanUpAcl = Object.values(acls.value).map(({ uid, acl }) =>
        inject("ContainerManagementPersistence", uid).setAccessControlList(
            acl.filter(
                ({ subject, verb }) =>
                    subject !== userUid || ![...DELEGATION_VERBS, ...SHARED_CONTAINERS_VERBS].includes(verb)
            )
        )
    );
    const removeCopyImipRule = removeDelegateFromCopyImipMailboxRule(userUid);
    return Promise.all(cleanUpAcl, removeCopyImipRule);
};

const setAclForDelegate = (containerUid, acl, delegate) => {
    const previousAcl = Object.values(acls.value).find(({ uid }) => uid === containerUid).acl;
    const newAcl = previousAcl.filter(({ subject }) => subject != delegate).concat(acl);
    return inject("ContainerManagementPersistence", containerUid).setAccessControlList(newAcl);
};

export const setCalendarAcl = (acl, delegate) => setAclForDelegate(calendarUid.value, acl, delegate);
export const setMailboxAcl = (acl, delegate) => setAclForDelegate(mailboxUid.value, acl, delegate);
export const setTodoListAcl = (acl, delegate) => setAclForDelegate(todoListUid.value, acl, delegate);
export const setContactsAcl = (acl, delegate) => setAclForDelegate(addressBookUid.value, acl, delegate);

const DELEGATION_VERBS = [Verb.SendOnBehalf, Verb.SendAs];
const SHARED_CONTAINERS_VERBS = [Verb.Read, Verb.Write, Verb.Manage];

export const Container = {
    CALENDAR: "calendar",
    MAILBOX: "mailbox",
    TODO_LIST: "todolist",
    CONTACTS: "addressbook"
};

export const Right = {
    HAS_NO_RIGHTS: {
        verbs: [],
        shortText: () => i18n.t("preferences.has_no_rights"),
        text: () => i18n.t("preferences.has_no_rights"),
        level: 0
    },
    CAN_READ: {
        verbs: [Verb.Read],
        shortText: () => i18n.t("preferences.account.delegates.right.short.can_read"),
        text: container => i18n.t(`preferences.account.delegates.right.${container}.can_read`),
        level: 1
    },
    CAN_EDIT: {
        verbs: [Verb.Write],
        shortText: () => i18n.t("preferences.account.delegates.right.short.can_edit"),
        text: container => i18n.t(`preferences.account.delegates.right.${container}.can_edit`),
        level: 2
    },
    CAN_MANAGE_SHARES: {
        verbs: [Verb.Write, Verb.Manage],
        shortText: () => i18n.t("preferences.account.delegates.right.short.can_edit_and_manage_shares"),
        text: container => i18n.t(`preferences.account.delegates.right.${container}.can_edit_and_manage_shares`),
        level: 3
    }
};

export const rightToAcl = (right, subject) => {
    return right.verbs.map(verb => ({ verb, subject }));
};

const orderedRights = Object.values(Right).sort((a, b) => b.level - a.level);

export const aclToRight = (acl, delegate, defaultRight, isNew) => {
    if (!acl || !delegate) {
        return defaultRight;
    }

    const filteredAclVerbs = acl
        .filter(({ subject, verb }) => subject === delegate && SHARED_CONTAINERS_VERBS.includes(verb))
        .map(({ verb }) => verb);

    for (const right of orderedRights) {
        const rightVerbs = right.verbs;
        if (without(rightVerbs, ...filteredAclVerbs).length === 0) {
            return isNew ? highestRight(defaultRight, right) : right;
        }
    }
};

export const highestRight = (defaultRight, right) => {
    const max = (defaultRight, right) => (!right || defaultRight.level > right.level ? defaultRight : right);
    return max(defaultRight, right);
};

const getRight = (acl, delegate) => {
    return aclToRight(acl, delegate, Right.HAS_NO_RIGHTS, false);
};
export const getCalendarRight = delegate => getRight(acls.value.calendar.acl, delegate);
export const getTodoListRight = delegate => getRight(acls.value.todoList.acl, delegate);
export const getMessageRight = delegate => getRight(acls.value.mailbox.acl, delegate);
export const getContactsRight = delegate => getRight(acls.value.addressBook.acl, delegate);

let cachedMailboxFilter;
const getMailboxFilter = async userId => {
    if (!cachedMailboxFilter) {
        cachedMailboxFilter = await inject("MailboxesPersistence").getMailboxFilter(userId);
    }
    return cachedMailboxFilter;
};

export const addDelegateToCopyImipMailboxRule = async ({ uid, address }) => {
    const mailboxFilter = await getMailboxFilter(getUserId());
    let copyImipMailboxRule = mailboxFilter.rules.find(matchCopyImipMailboxRule);
    const copyImipAction = {
        name: "REDIRECT",
        keepCopy: true,
        emails: [address],
        clientProperties: { type: "delegation", delegate: uid }
    };
    if (!copyImipMailboxRule) {
        copyImipMailboxRule = {
            actions: [copyImipAction]
        };
        mailboxFilter.rules.push(copyImipMailboxRule);
    } else if (!copyImipMailboxRule.actions.some(a => matchCopyImipActionForDelegate(a, uid))) {
        copyImipMailboxRule.actions.push(copyImipAction);
    }

    Object.assign(copyImipMailboxRule, {
        name: "Copy iMIP to Delegates",
        client: "system",
        active: true,
        conditions: [
            {
                filter: {
                    fields: ["headers.X-BM-Calendar"],
                    operator: "CONTAINS",
                    values: [calendarUid.value]
                },
                negate: false,
                operator: "AND",
                conditions: []
            }
        ]
    });

    await inject("MailboxesPersistence").setMailboxFilter(getUserId(), mailboxFilter);
    cachedMailboxFilter = mailboxFilter;
};

export const hasCopyImipMailboxRuleAction = async uid => {
    const mailboxFilter = await getMailboxFilter(getUserId());
    return mailboxFilter.rules
        .find(matchCopyImipMailboxRule)
        ?.actions.some(a => matchCopyImipActionForDelegate(a, uid));
};

export const removeDelegateFromCopyImipMailboxRule = async uid => {
    const mailboxFilter = await getMailboxFilter(getUserId());
    const copyImipMailboxRuleIndex = mailboxFilter.rules.findIndex(matchCopyImipMailboxRule);
    if (copyImipMailboxRuleIndex > -1) {
        const copyImipMailboxRule = mailboxFilter.rules[copyImipMailboxRuleIndex];
        const filteredActions = copyImipMailboxRule.actions.filter(
            ({ clientProperties: { type, delegate } }) => type !== "delegation" || delegate !== uid
        );
        if (filteredActions.length < copyImipMailboxRule.actions.length) {
            if (filteredActions.length === 0) {
                mailboxFilter.rules.splice(copyImipMailboxRuleIndex, 1);
            } else if (filteredActions.length < copyImipMailboxRule.actions.length) {
                copyImipMailboxRule.actions = filteredActions;
            }
            inject("MailboxesPersistence").setMailboxFilter(getUserId(), mailboxFilter);
            cachedMailboxFilter = null;
        }
    }
};

const matchCopyImipMailboxRule = rule => {
    return (
        rule.active === true &&
        rule.client === "system" &&
        rule.conditions.some(({ filter: { fields, operator, values } }) =>
            fields.some(
                f => f === "headers.X-BM-Calendar" && operator === "CONTAINS" && values[0]?.includes(calendarUid.value)
            )
        ) &&
        rule.actions.some(
            ({ name, emails, keepCopy, clientProperties: { type, delegate } }) =>
                name === "REDIRECT" && emails?.length && keepCopy === true && type === "delegation" && delegate
        )
    );
};

const matchCopyImipActionForDelegate = ({ clientProperties: { type, delegate } }, delegateUid) =>
    type === "delegation" && delegate === delegateUid;
