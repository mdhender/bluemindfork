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
        event = EventHelper.adapt(event, getters.currentMailbox.key);
    }
    commit("setCurrentEvent", event);
}

async function setEventStatus({ state, commit, getters }, status) {
    const uid = getters.currentMailbox.key;
    const previousStatus = state.currentEvent.status;
    try {
        commit("setCurrentEventStatus", { status, uid });
        await injector
            .getProvider("CalendarPersistence")
            .get()
            .update(state.currentEvent.uid, state.currentEvent.serverEvent.value, true);
    } catch {
        commit("setCurrentEventStatus", { status: previousStatus, uid });
    }
}

const mutations = {
    setCurrentEvent(state, event) {
        state.currentEvent = event;
    },
    setCurrentEventStatus(state, { status, uid }) {
        state.currentEvent.status = status;

        state.currentEvent.serverEvent.value.main.attendees.find(
            a => a.dir && a.dir.split("/").pop() === uid
        ).partStatus = status;
    }
};

const state = {
    currentEvent: null
};

export default { actions, mutations, state };
