//FIXME: move to new store

import EventHelper from "./EventHelper";
import injector from "@bluemind/inject";

const actions = {
    fetchEvent,
    setEventStatus
};

async function fetchEvent({ commit }, eventUid) {
    let event = await injector
        .getProvider("CalendarPersistence")
        .get()
        .getComplete(eventUid);
    if (event) {
        event = EventHelper.adapt(event);
    }
    commit("setCurrentEvent", event);
}

async function setEventStatus({ state, commit }, status) {
    commit("setCurrentEventStatus", status);
    return await injector
        .getProvider("CalendarPersistence")
        .get()
        .update(state.currentEvent.uid, state.currentEvent.serverEvent.value, true);
}

const mutations = {
    setCurrentEvent(state, event) {
        state.currentEvent = event;
    },
    setCurrentEventStatus(state, status) {
        state.currentEvent.status = status;

        const userSession = injector.getProvider("UserSession").get();
        state.currentEvent.serverEvent.value.main.attendees.find(
            a => a.mailto === userSession.defaultEmail
        ).partStatus = status;
    }
};

const state = {
    currentEvent: null
};

export default { actions, mutations, state };
