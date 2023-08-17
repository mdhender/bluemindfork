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

export const delegations = computed(() => {
    return Object.values(acls.value)
        .map(({ uid, acl }) =>
            acl?.filter(({ verb }) => [Verb.SendOnBehalf, Verb.SendAs].includes(verb)).map(acl => ({ uid, acl }))
        )
        .flatMap(r => r)
        .filter(Boolean);
});
