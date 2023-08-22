import { computed, ref, watch } from "vue";
import { Verb } from "@bluemind/core.container.api";
import { inject } from "@bluemind/inject";
import store from "@bluemind/store";

const getContainers = () => store.state.preferences.containers;
const isDefaultContainer = c => Boolean(c.defaultContainer);

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
        .map(({ uid, acl }) =>
            acl?.filter(({ verb }) => [Verb.SendOnBehalf, Verb.SendAs].includes(verb)).map(ac => ({ uid, ac }))
        )
        .flatMap(r => r)
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
    return Promise.all(
        Object.values(acls.value).map(({ uid, acl }) =>
            inject("ContainerManagementPersistence", uid).setAccessControlList(
                acl.filter(
                    ({ subject, verb }) => subject !== userUid || ![Verb.SendOnBehalf, Verb.SendAs].includes(verb)
                )
            )
        )
    );
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
