import {
    adapt,
    ContainerType,
    containerToSubscription,
    isManaged
} from "../../components/preferences/fields/customs/ContainersManagement/container";
import { Verb } from "@bluemind/core.container.api";
import { inject } from "@bluemind/inject";

const state = {
    myCalendars: [],
    otherCalendars: [], // includes calendars I subscribe to and those I manage (excluding mines)
    myMailboxContainer: null,
    otherMailboxesContainers: [], // includes mailboxes I subscribe to and those I manage (excluding mine)
    myAddressbooks: [],
    otherAddressbooks: [], // includes addressbooks I subscribe to and those I manage (excluding mine)
    myTodoLists: [],
    otherTodoLists: []
};

const actions = {
    async FETCH_CONTAINERS({ commit }, subscriptions) {
        const [calendars, mailboxes, addressbooks, todoLists] = await Promise.all([
            getContainers(ContainerType.CALENDAR, subscriptions),
            getContainers(ContainerType.MAILBOX, subscriptions),
            getContainers(ContainerType.ADDRESSBOOK, subscriptions),
            getContainers(ContainerType.TODOLIST, subscriptions)
        ]);
        commit("SET_CALENDARS", calendars);
        commit("SET_MAILBOX_CONTAINERS", mailboxes);
        commit("SET_ADDRESSBOOKS", addressbooks);
        commit("SET_TODO_LISTS", todoLists);
    },
    async SUBSCRIBE_TO_CONTAINERS({ commit }, containers) {
        const userId = inject("UserSession").userId;
        await inject("UserSubscriptionPersistence").subscribe(
            userId,
            containers.map(container => ({ containerUid: container.uid, offlineSync: container.offlineSync }))
        );
        const subscriptions = containers.map(containerToSubscription);
        commit("ADD_SUBSCRIPTIONS", subscriptions);
    }
};

const mutations = {
    // calendars
    SET_CALENDARS: (state, { myContainers, otherContainers }) => {
        state.myCalendars = myContainers;
        state.otherCalendars = otherContainers;
    },
    ADD_PERSONAL_CALENDAR: (state, myCalendar) => {
        state.myCalendars.push(myCalendar);
    },
    REMOVE_PERSONAL_CALENDAR: (state, calendarUid) => {
        const index = state.myCalendars.findIndex(myCal => myCal.uid === calendarUid);
        if (index !== -1) {
            state.myCalendars.splice(index, 1);
        }
    },
    UPDATE_PERSONAL_CALENDAR: (state, calendar) => {
        const index = state.myCalendars.findIndex(myCal => myCal.uid === calendar.uid);
        if (index !== -1) {
            state.myCalendars.splice(index, 1, calendar);
        }
    },
    ADD_OTHER_CALENDARS: (state, calendars) => {
        state.otherCalendars.push(...calendars);
    },
    REMOVE_OTHER_CALENDAR: (state, uid) => {
        const index = state.otherCalendars.findIndex(otherCal => otherCal.uid === uid);
        if (index !== -1) {
            state.otherCalendars.splice(index, 1);
        }
    },
    UPDATE_OTHER_CALENDAR: (state, calendar) => {
        const index = state.otherCalendars.findIndex(otherCal => otherCal.uid === calendar.uid);
        if (index !== -1) {
            state.otherCalendars.splice(index, 1, calendar);
        }
    },

    // mailboxes
    SET_MAILBOX_CONTAINERS: (state, { myContainers, otherContainers }) => {
        state.myMailboxContainer = myContainers[0];
        state.otherMailboxesContainers = otherContainers;
    },
    ADD_OTHER_MAILBOXES: (state, mailboxes) => {
        state.otherMailboxesContainers.push(...mailboxes);
    },
    REMOVE_OTHER_MAILBOX_CONTAINER: (state, containerUid) => {
        const index = state.otherMailboxesContainers.findIndex(container => container.uid === containerUid);
        if (index !== -1) {
            state.otherMailboxesContainers.splice(index, 1);
        }
    },
    UPDATE_OTHER_MAILBOX_CONTAINER: (state, updatedContainer) => {
        const index = state.otherMailboxesContainers.findIndex(
            mailboxContainer => mailboxContainer.uid === updatedContainer.uid
        );
        if (index !== -1) {
            state.otherMailboxesContainers.splice(index, 1, updatedContainer);
        }
    },

    // addressbooks
    SET_ADDRESSBOOKS: (state, { myContainers, otherContainers }) => {
        state.myAddressbooks = myContainers;
        state.otherAddressbooks = otherContainers;
    },
    ADD_PERSONAL_ADDRESSBOOK: (state, myAddressbook) => {
        state.myAddressbooks.push(myAddressbook);
    },
    REMOVE_PERSONAL_ADDRESSBOOK: (state, addressbookUid) => {
        const index = state.myAddressbooks.findIndex(myAddressbook => myAddressbook.uid === addressbookUid);
        if (index !== -1) {
            state.myAddressbooks.splice(index, 1);
        }
    },
    UPDATE_PERSONAL_ADDRESSBOOK: (state, addressbook) => {
        const index = state.myAddressbooks.findIndex(myAddressbook => myAddressbook.uid === addressbook.uid);
        if (index !== -1) {
            state.myAddressbooks.splice(index, 1, addressbook);
        }
    },
    ADD_OTHER_ADDRESSBOOK: (state, addressbooks) => {
        state.otherAddressbooks.push(...addressbooks);
    },
    REMOVE_OTHER_ADDRESSBOOK: (state, uid) => {
        const index = state.otherAddressbooks.findIndex(otherAddressbook => otherAddressbook.uid === uid);
        if (index !== -1) {
            state.otherAddressbooks.splice(index, 1);
        }
    },
    UPDATE_OTHER_ADDRESSBOOK: (state, addressbook) => {
        const index = state.otherAddressbooks.findIndex(otherAddressbook => otherAddressbook.uid === addressbook.uid);
        if (index !== -1) {
            state.otherAddressbooks.splice(index, 1, addressbook);
        }
    },

    // todo lists
    SET_TODO_LISTS: (state, { myContainers, otherContainers }) => {
        state.myTodoLists = myContainers;
        state.otherTodoLists = otherContainers;
    },
    ADD_PERSONAL_TODO_LIST: (state, myTodoList) => {
        state.myTodoLists.push(myTodoList);
    },
    REMOVE_PERSONAL_TODO_LIST: (state, todoListUid) => {
        const index = state.myTodoLists.findIndex(myTodoList => myTodoList.uid === todoListUid);
        if (index !== -1) {
            state.myTodoLists.splice(index, 1);
        }
    },
    UPDATE_PERSONAL_TODO_LIST: (state, todoList) => {
        const index = state.myTodoLists.findIndex(myTodoList => myTodoList.uid === todoList.uid);
        if (index !== -1) {
            state.myTodoLists.splice(index, 1, todoList);
        }
    },
    ADD_OTHER_TODO_LIST: (state, todoLists) => {
        state.otherTodoLists.push(...todoLists);
    },
    REMOVE_OTHER_TODO_LIST: (state, uid) => {
        const index = state.otherTodoLists.findIndex(list => list.uid === uid);
        if (index !== -1) {
            state.otherTodoLists.splice(index, 1);
        }
    },
    UPDATE_OTHER_TODO_LIST: (state, todoList) => {
        const index = state.otherTodoLists.findIndex(list => list.uid === todoList.uid);
        if (index !== -1) {
            state.otherTodoLists.splice(index, 1, todoList);
        }
    }
};

export default {
    actions,
    mutations,
    state
};

async function getContainers(type, subscriptions) {
    const userId = inject("UserSession").userId;

    const subscribedContainerUids = subscriptions
        .filter(sub => sub.value.containerType === type && sub.value.owner !== userId)
        .map(sub => sub.value.containerUid);

    const [myContainers, managedContainers, otherSubscribed] = await Promise.all([
        inject("ContainersPersistence").all({ type, owner: userId }),
        inject("ContainersPersistence").all({ type, verb: [Verb.Manage, Verb.All] }),
        inject("ContainersPersistence").getContainers(subscribedContainerUids)
    ]);

    const otherManagedContainers = managedContainers.filter(container => container.owner !== userId);
    const subscribedNotManaged = otherSubscribed.filter(container => !isManaged(container));

    return {
        myContainers: myContainers.map(adapt).sort(sortMine),
        otherContainers: otherManagedContainers
            .concat(subscribedNotManaged)
            .map(adapt)
            .sort((a, b) => a.name.localeCompare(b.name))
    };
}

function sortMine(container1, container2) {
    if (container1.defaultContainer && !container2.defaultContainer) {
        return -1;
    }
    if (!container1.defaultContainer && container2.defaultContainer) {
        return 1;
    }
    return container1.name.localeCompare(container2.name);
}
