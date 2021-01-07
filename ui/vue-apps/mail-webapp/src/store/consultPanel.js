import { inject } from "@bluemind/inject";
import EventHelper from "./helpers/EventHelper";
import { FETCH_EVENT, SET_EVENT_STATUS, ACCEPT_COUNTER_EVENT, DECLINE_COUNTER_EVENT } from "~actions";
import {
    SET_BLOCK_REMOTE_IMAGES,
    SET_CURRENT_EVENT,
    SET_CURRENT_EVENT_STATUS,
    SET_SHOW_REMOTE_IMAGES_ALERT
} from "~mutations";

export default {
    state: {
        currentEvent: null,
        remoteImages: {
            showAlert: false,
            mustBeBlocked: false
        }
    },
    actions: {
        async [FETCH_EVENT]({ commit }, { message, mailbox }) {
            let event;
            if (message.eventInfo.icsUid) {
                event = await inject("CalendarPersistence").getByIcsUid(message.eventInfo.icsUid);
                event = event ? event[0] : event;
            } else {
                event = await inject("CalendarPersistence").getComplete(message.eventInfo.eventUid);
            }

            if (event) {
                event = EventHelper.adapt(event, mailbox.owner, message.from.address, message.eventInfo.recuridIsoDate);
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
        },

        [ACCEPT_COUNTER_EVENT]({ state, commit }) {
            return updateCounterEvent({ state, commit }, EventHelper.applyCounter);
        },

        [DECLINE_COUNTER_EVENT]({ state, commit }) {
            return updateCounterEvent({ state, commit }, EventHelper.removeCounter);
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
        },
        [SET_SHOW_REMOTE_IMAGES_ALERT](state, showAlert) {
            state.remoteImages.showAlert = showAlert;
        },
        [SET_BLOCK_REMOTE_IMAGES](state, mustBeBlocked) {
            state.remoteImages.mustBeBlocked = mustBeBlocked;
        }
    }
};

async function updateCounterEvent({ state, commit }, updateFunction) {
    let updatedEvent = JSON.parse(JSON.stringify(state.currentEvent));

    const recuridIsoDate = state.currentEvent.counter.occurrence
        ? state.currentEvent.counter.occurrence.recurid.iso8601
        : undefined;
    updateFunction(updatedEvent, state.currentEvent.counter.originator, recuridIsoDate);

    updatedEvent = EventHelper.adapt(
        updatedEvent.serverEvent,
        state.currentEvent.mailboxOwner,
        state.currentEvent.counter.originator
    );

    commit(SET_CURRENT_EVENT, updatedEvent);
    await inject("CalendarPersistence").update(state.currentEvent.uid, updatedEvent.serverEvent.value, true);
}
