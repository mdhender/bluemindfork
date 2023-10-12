import without from "lodash.without";
import { computed, ref, watch } from "vue";
import { Verb } from "@bluemind/core.container.api";
import { Flag } from "@bluemind/email";
import i18n from "@bluemind/i18n";
import { inject } from "@bluemind/inject";
import store from "@bluemind/store";

export function useDelegation() {
    const DELEGATION_VERBS = [Verb.SendOnBehalf, Verb.SendAs];
    const SHARED_CONTAINERS_VERBS = [Verb.Read, Verb.Write, Verb.Manage];

    const Container = {
        CALENDAR: "calendar",
        MAILBOX: "mailbox",
        TODO_LIST: "todolist",
        CONTACTS: "addressbook"
    };

    const Right = {
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

    /** Delegates and their rights: { delegatUid1: {containerUid1: [verb1, verb2]} } */
    const delegates = ref({});

    const updateDelegates = acls => {
        const mailboxAcl = acls[mailboxUid.value];
        const delegatesUids = mailboxAcl?.flatMap(({ subject, verb }) =>
            DELEGATION_VERBS.includes(verb) ? subject : []
        );
        const tmp = {};
        if (delegatesUids?.length) {
            Object.entries(acls).forEach(([containerUid, acl]) => {
                acl.forEach(({ subject, verb }) => {
                    if (delegatesUids.includes(subject)) {
                        if (!tmp[subject]) {
                            tmp[subject] = {};
                        }
                        if (!tmp[subject][containerUid]) {
                            tmp[subject][containerUid] = [];
                        }
                        tmp[subject][containerUid].push(verb);
                    }
                });
            });
        }
        delegates.value = tmp;
    };

    watch(store.state.preferences.containers.acls, updateDelegates, { immediate: true });

    const fetchAclIfNotLoaded = uid => store.dispatch("preferences/FETCH_ACL_IF_NOT_LOADED", uid);

    const containerWatcher = value => {
        if (value) {
            fetchAclIfNotLoaded(value);
        }
    };
    watch(mailboxUid, containerWatcher, { immediate: true });
    watch(addressBookUid, containerWatcher, { immediate: true });
    watch(todoListUid, containerWatcher, { immediate: true });
    watch(calendarUid, containerWatcher, { immediate: true });

    const fetchAcl = uid => store.dispatch("preferences/FETCH_ACL", uid);

    const fetchAcls = () => {
        const promises = [];
        if (mailboxUid.value) {
            promises.push(fetchAcl(mailboxUid.value));
        }
        if (calendarUid.value) {
            promises.push(fetchAcl(calendarUid.value));
        }
        if (addressBookUid.value) {
            promises.push(fetchAcl(addressBookUid.value));
        }
        if (todoListUid.value) {
            promises.push(fetchAcl(todoListUid.value));
        }
        return Promise.all(promises);
    };

    const containerUids = computed(() => [
        mailboxUid.value,
        calendarUid.value,
        addressBookUid.value,
        todoListUid.value
    ]);

    const getAcl = uid => store.state.preferences.containers.acls[uid];
    const getCalendarAcl = () => getAcl(calendarUid.value);
    const getContactsAcl = () => getAcl(addressBookUid.value);
    const getMailboxAcl = () => getAcl(mailboxUid.value);
    const getTodoListAcl = () => getAcl(todoListUid.value);

    const delegationTypes = computed(() => {
        const delegationTypes = {};
        Object.keys(delegates.value).forEach(uid => {
            delegationTypes[uid] = delegates.value[uid][mailboxUid.value]?.some(verbs => verbs.includes(Verb.SendAs))
                ? Verb.SendAs
                : Verb.SendOnBehalf;
        });
        return delegationTypes;
    });

    const removeDelegate = userUid => {
        const cleanUpAcl = containerUids.value.map(uid => {
            if (uid) {
                inject("ContainerManagementPersistence", uid).setAccessControlList(
                    getAcl(uid).filter(
                        ({ subject, verb }) =>
                            subject !== userUid || ![...DELEGATION_VERBS, ...SHARED_CONTAINERS_VERBS].includes(verb)
                    )
                );
            }
        });
        const removeCopyImipRule = removeDelegateFromCopyImipMailboxRule(userUid);
        return Promise.all(cleanUpAcl, removeCopyImipRule);
    };

    const setAclForDelegate = (containerUid, acl, delegate) => {
        const previousAcl = getAcl(containerUid);
        const newAcl = previousAcl.filter(({ subject }) => subject != delegate).concat(acl);
        return inject("ContainerManagementPersistence", containerUid).setAccessControlList(newAcl);
    };

    const setCalendarAcl = (acl, delegate) => setAclForDelegate(calendarUid.value, acl, delegate);
    const setMailboxAcl = (acl, delegate) => setAclForDelegate(mailboxUid.value, acl, delegate);
    const setTodoListAcl = (acl, delegate) => setAclForDelegate(todoListUid.value, acl, delegate);
    const setContactsAcl = (acl, delegate) => setAclForDelegate(addressBookUid.value, acl, delegate);

    const rightToAcl = (right, subject) => {
        return right.verbs.map(verb => ({ verb, subject }));
    };

    const orderedRights = Object.values(Right).sort((a, b) => b.level - a.level);

    const toRight = (filteredVerbs, defaultRight, isNew) => {
        for (const right of orderedRights) {
            const rightVerbs = right.verbs;
            if (without(rightVerbs, ...filteredVerbs).length === 0) {
                return isNew ? highestRight(defaultRight, right) : right;
            }
        }
    };

    const aclToRight = (acl, delegate, defaultRight, isNew) => {
        if (!acl || !delegate) {
            return defaultRight;
        }
        const filteredAclVerbs = acl
            .filter(({ subject, verb }) => subject === delegate && SHARED_CONTAINERS_VERBS.includes(verb))
            .map(({ verb }) => verb);
        return toRight(filteredAclVerbs, defaultRight, isNew);
    };

    const verbsToRight = (verbs, defaultRight, isNew) => {
        if (!verbs?.length) {
            return defaultRight;
        }
        const filteredVerbs = verbs.filter(verb => SHARED_CONTAINERS_VERBS.includes(verb));
        return toRight(filteredVerbs, defaultRight, isNew);
    };

    const highestRight = (defaultRight, right) => {
        const max = (defaultRight, right) => (!right || defaultRight.level > right.level ? defaultRight : right);
        return max(defaultRight, right);
    };

    const getRight = verbs => {
        return verbsToRight(verbs, Right.HAS_NO_RIGHTS, false);
    };
    const getCalendarRight = delegate => getRight(delegates.value[delegate][calendarUid.value]);
    const getTodoListRight = delegate => getRight(delegates.value[delegate][todoListUid.value]);
    const getMessageRight = delegate => getRight(delegates.value[delegate][mailboxUid.value]);
    const getContactsRight = delegate => getRight(delegates.value[delegate][addressBookUid.value]);

    const delegatesWithImipRule = ref([]);

    const matchCopyImipMailboxRule = rule => {
        return (
            rule.active === true &&
            rule.client === "system" &&
            rule.conditions.some(({ filter: { fields, operator, values } }) =>
                fields.some(
                    f =>
                        f === "headers.X-BM-Calendar" &&
                        operator === "CONTAINS" &&
                        values[0]?.includes(calendarUid.value)
                )
            ) &&
            rule.actions.some(
                ({ name, emails, clientProperties: { type, delegate } }) =>
                    name === "REDIRECT" && emails?.length && type === "delegation" && delegate
            )
        );
    };

    watch(
        store.state.preferences.mailboxFilter,
        ({ rules }) => {
            let copyImipMailboxRule = rules.find(matchCopyImipMailboxRule);
            delegatesWithImipRule.value =
                copyImipMailboxRule?.actions.map(({ clientProperties: { delegate } }) => delegate) || [];
        },
        { immediate: true }
    );

    const setFlagsAction = {
        name: "SET_FLAGS",
        flags: [Flag.READ_ONLY_EVENT],
        clientProperties: { type: "delegation" }
    };

    const addDelegateToCopyImipMailboxRule = async ({ uid, address, receiveImipOption }) => {
        const mailboxFilter = store.state.preferences.mailboxFilter;
        let copyImipMailboxRule = mailboxFilter.rules.find(matchCopyImipMailboxRule);
        const copyImipAction = {
            name: "REDIRECT",
            keepCopy: receiveImipOption !== receiveImipOptions.ONLY_DELEGATE,
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

        if (
            receiveImipOption === receiveImipOptions.COPY &&
            !copyImipMailboxRule.actions.some(
                ({ name, clientProperties }) => clientProperties.type === "delegation" && name === "SET_FLAGS"
            )
        ) {
            copyImipMailboxRule.actions.push(setFlagsAction);
        }

        store.dispatch("preferences/SAVE_RULES", mailboxFilter.rules);
    };

    const receiveImipOptions = { ONLY_DELEGATE: 0, BOTH: 1, COPY: 2 };

    const updateReceiveImipOption = async receiveImipOption => {
        const mailboxFilter = store.state.preferences.mailboxFilter;
        const copyImipMailboxRule = mailboxFilter.rules.find(matchCopyImipMailboxRule);
        const setFlagsActionIndex = copyImipMailboxRule.actions.findIndex(
            ({ clientProperties, name }) => clientProperties?.type === "delegation" && name === "SET_FLAGS"
        );

        // add or remove the only one SET_FLAGS action
        if (receiveImipOptions.COPY === receiveImipOption && setFlagsActionIndex === -1) {
            copyImipMailboxRule.actions.push(setFlagsAction);
        } else if (receiveImipOptions.COPY !== receiveImipOption && setFlagsActionIndex !== -1) {
            copyImipMailboxRule.actions.splice(setFlagsActionIndex, 1);
        }

        // update all REDIRECT actions
        copyImipMailboxRule.actions.forEach(a => {
            if (a.clientProperties?.type === "delegation" && a.name === "REDIRECT") {
                a.keepCopy = receiveImipOption !== receiveImipOptions.ONLY_DELEGATE;
            }
        });

        store.dispatch("preferences/SAVE_RULES", mailboxFilter.rules);
    };

    const computeReceiveImipOption = async () => {
        const mailboxFilter = store.state.preferences.mailboxFilter;
        const copyImipMailboxRule = mailboxFilter.rules.find(matchCopyImipMailboxRule);
        if (copyImipMailboxRule) {
            let keepCopy = false;
            // reminder: only one SET_FLAGS action but several REDIRECT actions
            for (const action of copyImipMailboxRule.actions) {
                if (action.clientProperties?.type === "delegation") {
                    if (action.name === "SET_FLAGS") {
                        return receiveImipOptions.COPY;
                    }
                    if (action.name === "REDIRECT" && action.keepCopy) {
                        keepCopy = true;
                    }
                }
            }
            return keepCopy ? receiveImipOptions.BOTH : receiveImipOptions.ONLY_DELEGATE;
        }
        return receiveImipOptions.BOTH;
    };

    const hasCopyImipMailboxRuleAction = (...uids) => {
        return delegatesWithImipRule.value.some(delegate => uids.includes(delegate));
    };

    const countDelegatesHavingTheCopyImipRule = async (...uids) => {
        const mailboxFilter = store.state.preferences.mailboxFilter;
        return (
            mailboxFilter.rules
                .find(matchCopyImipMailboxRule)
                ?.actions.filter(a => uids.some(uid => matchCopyImipActionForDelegate(a, uid))).length || 0
        );
    };

    const removeDelegateFromCopyImipMailboxRule = async uid => {
        const mailboxFilter = store.state.preferences.mailboxFilter;
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
                store.dispatch("preferences/SAVE_RULES", mailboxFilter.rules);
            }
        }
    };

    const matchCopyImipActionForDelegate = ({ clientProperties: { type, delegate } }, delegateUid) =>
        type === "delegation" && delegate === delegateUid;

    return {
        aclToRight,
        addDelegateToCopyImipMailboxRule,
        computeReceiveImipOption,
        Container,
        countDelegatesHavingTheCopyImipRule,
        delegates,
        delegationTypes,
        fetchAcls,
        getCalendarAcl,
        getCalendarRight,
        getContactsAcl,
        getContactsRight,
        getMailboxAcl,
        getMessageRight,
        getTodoListAcl,
        getTodoListRight,
        hasCopyImipMailboxRuleAction,
        highestRight,
        receiveImipOptions,
        removeDelegate,
        removeDelegateFromCopyImipMailboxRule,
        Right,
        rightToAcl,
        setCalendarAcl,
        setContactsAcl,
        setMailboxAcl,
        setTodoListAcl,
        updateReceiveImipOption
    };
}
