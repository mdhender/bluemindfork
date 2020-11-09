import { inject } from "@bluemind/inject";
import EventHelper from "./helpers/EventHelper";
import { FETCH_EVENT, SET_EVENT_STATUS } from "~actions";
import { SET_CURRENT_EVENT, SET_CURRENT_EVENT_STATUS } from "~mutations";

export default {
    state: {
        currentEvent: null
    },
    actions: {
        async [FETCH_EVENT]({ commit }, { eventUid, mailbox }) {
            let event = await inject("CalendarPersistence").getComplete(eventUid);
            if (event) {
                event = EventHelper.adapt(event, mailbox.owner);
            }
            commit(SET_CURRENT_EVENT, event);
        },

        async [SET_EVENT_STATUS]({ state, commit }, { status, mailbox }) {
            const uid = mailbox.owner;
            const previousStatus = state.currentEvent.status;
            try {
                commit(SET_CURRENT_EVENT_STATUS, { status, uid });
                await inject("CalendarPersistence").update(
                    state.currentEvent.uid,
                    state.currentEvent.serverEvent.value,
                    true
                );
            } catch {
                commit(SET_CURRENT_EVENT_STATUS, { status: previousStatus, uid });
            }
        }
    },
    mutations: {
        [SET_CURRENT_EVENT](state, event) {
            state.currentEvent = event;
        },
        [SET_CURRENT_EVENT_STATUS](state, { status, uid }) {
            state.currentEvent.status = status;

            state.currentEvent.serverEvent.value.main.attendees.find(
                a => a.dir && a.dir.split("/").pop() === uid
            ).partStatus = status;
        }
    }
};
