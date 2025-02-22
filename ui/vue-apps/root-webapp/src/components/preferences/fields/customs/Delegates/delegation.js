import without from "lodash.without";
import cloneDeep from "lodash.clonedeep";
import { computed, ref, watch } from "vue";
import { Verb } from "@bluemind/core.container.api";
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
        const delegateUids = mailboxAcl?.flatMap(({ subject, verb }) =>
            subject !== getUserId() && DELEGATION_VERBS.includes(verb) ? subject : []
        );
        const tmp = {};
        if (delegateUids?.length) {
            Object.entries(acls).forEach(([containerUid, acl]) => {
                acl.forEach(({ subject, verb }) => {
                    if (delegateUids.includes(subject)) {
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
        const cleanUpAcl = containerUids.value.map(containerUid => {
            if (containerUid) {
                const acl = getAcl(containerUid).filter(
                    ({ subject, verb }) =>
                        subject !== userUid || ![...DELEGATION_VERBS, ...SHARED_CONTAINERS_VERBS].includes(verb)
                );
                return store.dispatch("preferences/UPDATE_ACL", { containerUid, acl });
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

    const setContainerAcl = (container, delegate, right, extraVerbs) => {
        let acl = rightToAcl(right, delegate);
        if (extraVerbs?.length) {
            acl = acl.concat(extraVerbs.map(verb => ({ subject: delegate, verb })));
        }
        let containerUid;
        switch (container) {
            case Container.CALENDAR:
                containerUid = calendarUid.value;
                break;
            case Container.MAILBOX:
                containerUid = mailboxUid.value;
                break;
            case Container.TODO_LIST:
                containerUid = todoListUid.value;
                break;
            case Container.CONTACTS:
                containerUid = addressBookUid.value;
                break;
        }
        return setAclForDelegate(containerUid, acl, delegate);
    };

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

    const addDelegateToCopyImipMailboxRule = async ({ uid, receiveImipOption }) => {
        const imipRule = cloneDeep(store.state.preferences.mailboxFilter.imipRule) || { delegateUids: [] };
        if (!imipRule.delegateUids.includes(uid)) {
            imipRule.delegateUids.push(uid);
            imipRule.keepCopy = receiveImipOption !== receiveImipOptions.ONLY_DELEGATE;
            store.dispatch("preferences/SAVE_IMIP_RULE", { imipRule, calendarUid: calendarUid.value });
        }
    };

    const receiveImipOptions = { ONLY_DELEGATE: 0, BOTH: 1, COPY: 2 };

    const updateReceiveImipOption = async receiveImipOption => {
        const imipRule = cloneDeep(store.state.preferences.mailboxFilter.imipRule);
        imipRule.keepCopy = receiveImipOption !== receiveImipOptions.ONLY_DELEGATE;
        imipRule.readOnly = receiveImipOption === receiveImipOptions.COPY;
        store.dispatch("preferences/SAVE_IMIP_RULE", { imipRule, calendarUid: calendarUid.value });
    };

    const computeReceiveImipOption = () => {
        const imipRule = store.state.preferences.mailboxFilter.imipRule;
        if (imipRule) {
            return imipRule.keepCopy && imipRule.readOnly
                ? receiveImipOptions.COPY
                : imipRule.keepCopy
                ? receiveImipOptions.BOTH
                : receiveImipOptions.ONLY_DELEGATE;
        }
        return receiveImipOptions.BOTH;
    };

    const hasCopyImipMailboxRuleAction = (...uids) => {
        return store.state.preferences.mailboxFilter.imipRule?.delegateUids.some(uid => uids.includes(uid));
    };

    const countDelegatesHavingTheCopyImipRule = () => {
        return store.state.preferences.mailboxFilter.imipRule?.delegateUids.length || 0;
    };

    const removeDelegateFromCopyImipMailboxRule = async uid => {
        const imipRule = cloneDeep(store.state.preferences.mailboxFilter.imipRule);
        const index = imipRule.delegateUids.indexOf(uid);
        if (index >= 0) {
            imipRule.delegateUids.splice(index, 1);
            store.dispatch("preferences/SAVE_IMIP_RULE", { imipRule, calendarUid: calendarUid.value });
        }
    };

    const canSeePrivateEvents = uid =>
        getCalendarAcl()?.some(({ subject, verb }) => subject === uid && verb === Verb.ReadExtended);

    const hasIncoherentCopyImipOption = (delegate, hasCopyImip, calendarRight) => {
        hasCopyImip = hasCopyImip !== undefined ? hasCopyImip : hasCopyImipMailboxRuleAction(delegate);
        const calendarVerbs =
            calendarRight !== undefined
                ? calendarRight.verbs
                : delegates.value[delegate] && delegates.value[delegate][calendarUid.value];
        return hasCopyImip && !calendarVerbs?.some(verb => [Verb.All, Verb.Manage, Verb.Write].includes(verb));
    };

    return {
        aclToRight,
        addDelegateToCopyImipMailboxRule,
        canSeePrivateEvents,
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
        hasIncoherentCopyImipOption,
        highestRight,
        receiveImipOptions,
        removeDelegate,
        removeDelegateFromCopyImipMailboxRule,
        Right,
        rightToAcl,
        setContainerAcl,
        updateReceiveImipOption
    };
}
