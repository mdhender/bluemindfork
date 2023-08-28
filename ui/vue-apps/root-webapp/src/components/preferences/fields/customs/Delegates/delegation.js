import without from "lodash.without";
import { computed, ref, watch } from "vue";
import { Verb } from "@bluemind/core.container.api";
import i18n from "@bluemind/i18n";
import { inject } from "@bluemind/inject";
import store from "@bluemind/store";

const getContainers = () => store.state.preferences.containers;
const isDefaultContainer = c => Boolean(c.defaultContainer);

const userId = inject("UserSession").userId;
const mailboxUid = computed(() => getContainers().myMailboxContainer.uid);
const calendarUid = computed(() => getContainers().myCalendars.find(isDefaultContainer)?.uid);
const addressBookUid = computed(() => getContainers().myAddressbooks.find(isDefaultContainer)?.uid);
const todoListUid = computed(() => getContainers().myTodoLists.find(isDefaultContainer)?.uid);

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
    return Object.values(acls.value)
        .map(({ uid, acl }) => acl?.filter(({ verb }) => DELEGATION_VERBS.includes(verb)).map(ac => ({ uid, ac })))
        .flat()
        .filter(Boolean);
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

export const Right = {
    NONE: { verbs: [], text: () => i18n.t("preferences.account.delegates.right.none") },
    REVIEWER: {
        verbs: [Verb.Read],
        text: () => i18n.t("preferences.account.delegates.right.reviewer"),
        textWithDescription: () =>
            i18n.t("preferences.account.delegates.right.reviewer.with_description", {
                reviewer: i18n.t("preferences.account.delegates.right.reviewer")
            })
    },
    AUTHOR: {
        verbs: [Verb.Read, Verb.Write],
        text: () => i18n.t("preferences.account.delegates.right.author"),
        textWithDescription: () =>
            i18n.t("preferences.account.delegates.right.author.with_description", {
                author: i18n.t("preferences.account.delegates.right.author")
            })
    },
    EDITOR: {
        verbs: [Verb.Read, Verb.Write, Verb.Manage],
        text: () => i18n.t("preferences.account.delegates.right.editor"),
        textWithDescription: () =>
            i18n.t("preferences.account.delegates.right.editor.with_description", {
                editor: i18n.t("preferences.account.delegates.right.editor")
            })
    }
};

export const rightToAcl = (right, subject) => {
    return right.verbs.map(verb => ({ verb, subject }));
};

const orderedRights = [Right.EDITOR, Right.AUTHOR, Right.REVIEWER, Right.NONE];

export const aclToRight = (acl, delegate, defaultRight) => {
    if (!acl || !delegate) {
        return defaultRight;
    }

    const filteredAclVerbs = acl
        .filter(({ subject, verb }) => subject === delegate && SHARED_CONTAINERS_VERBS.includes(verb))
        .map(({ verb }) => verb);

    for (const right of orderedRights) {
        const rightVerbs = right.verbs;
        if (without(rightVerbs, ...filteredAclVerbs).length === 0) {
            return defaultRight && right === Right.NONE ? defaultRight : right;
        }
    }
};

const getRight = (acl, delegate) => {
    return aclToRight(acl, delegate, Right.NONE);
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
    const mailboxFilter = await getMailboxFilter(userId);
    let copyImipMailboxRule = mailboxFilter.rules.find(matchCopyImipMailboxRule);
    const copyImipAction = {
        name: "REDIRECT",
        keepCopy: true,
        emails: [address],
        clientProperties: { type: "delegation", delegate: uid }
    };
    if (!copyImipMailboxRule) {
        copyImipMailboxRule = {
            name: "Copy iMIP to Delegates",
            client: "system",
            active: true,
            actions: [copyImipAction],
            conditions: [
                {
                    filter: {
                        fields: ["headers.X-BM-Event"],
                        operator: "CONTAINS",
                        values: [`containerUid=${calendarUid.value}`]
                    }
                }
            ]
        };
        mailboxFilter.rules.push(copyImipMailboxRule);
    } else if (!copyImipMailboxRule.actions.some(a => matchCopyImipActionForDelegate(a, uid))) {
        copyImipMailboxRule.actions.push(copyImipAction);
    }
    await inject("MailboxesPersistence").setMailboxFilter(userId, mailboxFilter);
    cachedMailboxFilter = mailboxFilter;
};

export const hasCopyImipMailboxRuleAction = async uid => {
    const mailboxFilter = await getMailboxFilter(userId);
    return mailboxFilter.rules
        .find(matchCopyImipMailboxRule)
        ?.actions.some(a => matchCopyImipActionForDelegate(a, uid));
};

export const removeDelegateFromCopyImipMailboxRule = async uid => {
    const mailboxFilter = await getMailboxFilter(userId);
    const copyImipMailboxRuleIndex = mailboxFilter.rules.findIndex(matchCopyImipMailboxRule);
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
        inject("MailboxesPersistence").setMailboxFilter(userId, mailboxFilter);
        cachedMailboxFilter = null;
    }
};

const matchCopyImipMailboxRule = rule => {
    return (
        rule.active === true &&
        rule.client === "system" &&
        rule.conditions.some(({ filter: { fields, operator, values } }) =>
            fields.some(
                f =>
                    f === "headers.X-BM-Event" &&
                    operator === "CONTAINS" &&
                    values[0] === `containerUid=${calendarUid.value}`
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
