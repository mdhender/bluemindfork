// FIXME: move to new store
// FIXME: add tests when moving to new store

import EventHelper from "./EventHelper";
import injector from "@bluemind/inject";

const actions = {
    fetchEvent,
    setEventStatus
};

async function fetchEvent({ commit, getters }, eventUid) {
    let event = await injector
        .getProvider("CalendarPersistence")
        .get()
        .getComplete(eventUid);
    if (event) {
        event = EventHelper.adapt(event, getters.currentMailbox.name);
    }
    commit("setCurrentEvent", event);
}

async function setEventStatus({ state, commit, getters }, status) {
    const userName = getters.currentMailbox.name;
    const previousStatus = state.currentEvent.status;
    commit("setCurrentEventStatus", { status, userName });
    return await injector
        .getProvider("CalendarPersistence")
        .get()
        .update(state.currentEvent.uid, state.currentEvent.serverEvent.value, true)
        .catch(() => commit("setCurrentEventStatus", { status: previousStatus, userName }));
}

const mutations = {
    setCurrentEvent(state, event) {
        state.currentEvent = event;
    },
    setCurrentEventStatus(state, { status, userName }) {
        state.currentEvent.status = status;

        const userSession = injector.getProvider("UserSession").get();
        const currentAddress = userName + "@" + userSession.domain;
        state.currentEvent.serverEvent.value.main.attendees.find(a => a.mailto === currentAddress).partStatus = status;
    }
};

const state = {
    currentEvent: null
};

export default { actions, mutations, state };
