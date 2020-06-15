//FIXME: move to new store

import EventHelper from "./EventHelper";
import injector from "@bluemind/inject";

const actions = {
    fetchEvent
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

const mutations = {
    setCurrentEvent(state, event) {
        state.currentEvent = event;
    }
};

const state = {
    currentEvent: null
};

export default { actions, mutations, state };
