import { inject } from "@bluemind/inject";
import EventHelper from "./helpers/EventHelper";

export const state = {
    consultPanel: {
        currentEvent: null
    }
};

export const actions = {
    async FETCH_EVENT({ commit, getters }, eventUid) {
        let event = await inject("CalendarPersistence").getComplete(eventUid);
        if (event) {
            event = EventHelper.adapt(event, getters["CURRENT_MAILBOX"].owner);
        }
        commit("SET_CURRENT_EVENT", event);
    },

    async SET_EVENT_STATUS({ state, commit, getters }, status) {
        const uid = getters["CURRENT_MAILBOX"].owner;
        const previousStatus = state.consultPanel.currentEvent.status;
        try {
            commit("SET_CURRENT_EVENT_STATUS", { status, uid });
            await inject("CalendarPersistence").update(
                state.consultPanel.currentEvent.uid,
                state.consultPanel.currentEvent.serverEvent.value,
                true
            );
        } catch {
            commit("SET_CURRENT_EVENT_STATUS", { status: previousStatus, uid });
        }
    }
};

export const mutations = {
    SET_CURRENT_EVENT(state, event) {
        state.consultPanel.currentEvent = event;
    },
    SET_CURRENT_EVENT_STATUS(state, { status, uid }) {
        state.consultPanel.currentEvent.status = status;

        state.consultPanel.currentEvent.serverEvent.value.main.attendees.find(
            a => a.dir && a.dir.split("/").pop() === uid
        ).partStatus = status;
    }
};
