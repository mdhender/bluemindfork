import { inject } from "@bluemind/inject";
import EventHelper from "./helpers/EventHelper";
import { FETCH_EVENT, SET_EVENT_STATUS, ACCEPT_COUNTER_EVENT, DECLINE_COUNTER_EVENT } from "~/actions";
import {
    RESET_ACTIVE_MESSAGE,
    SET_BLOCK_REMOTE_IMAGES,
    SET_CURRENT_EVENT,
    SET_CURRENT_EVENT_STATUS
} from "~/mutations";
import { LoadingStatus } from "~/model/loading-status";

export default {
    state: {
        currentEvent: { loading: LoadingStatus.NOT_LOADED },
        remoteImages: { mustBeBlocked: false }
    },
    actions: {
        async [FETCH_EVENT]({ commit }, { message, mailbox }) {
            commit(SET_CURRENT_EVENT, { loading: LoadingStatus.LOADING });
            let event;
            if (message.eventInfo.icsUid) {
                const events = await inject("CalendarPersistence").getByIcsUid(message.eventInfo.icsUid);
                event = findEvent(events, message.eventInfo.recuridIsoDate) || events[0];
            } else if (message.eventInfo.isResourceBooking) {
                event = await inject("CalendarPersistence", "calendar:" + message.eventInfo.resourceUid).getComplete(
                    message.eventInfo.eventUid
                );
            } else {
                event = await inject("CalendarPersistence").getComplete(message.eventInfo.eventUid);
            }

            if (event) {
                const mailboxOwner = message.eventInfo.isResourceBooking
                    ? message.eventInfo.resourceUid
                    : mailbox.owner;
                event = EventHelper.adapt(event, mailboxOwner, message.from.address, message.eventInfo.recuridIsoDate);
                commit(SET_CURRENT_EVENT, event);
            } else {
                commit(SET_CURRENT_EVENT, { loading: LoadingStatus.ERROR });
                throw "Event not found";
            }
        },

        async [SET_EVENT_STATUS]({ state, commit }, { message, status }) {
            const previousStatus = state.currentEvent.status;
            try {
                commit(SET_CURRENT_EVENT_STATUS, { status });
                const service = message.eventInfo.isResourceBooking
                    ? inject("CalendarPersistence", "calendar:" + message.eventInfo.resourceUid)
                    : inject("CalendarPersistence");
                await service.update(state.currentEvent.uid, state.currentEvent.serverEvent.value, true);
            } catch {
                commit(SET_CURRENT_EVENT_STATUS, { status: previousStatus });
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
        [SET_CURRENT_EVENT_STATUS](state, { status }) {
            state.currentEvent.status = status;
            EventHelper.setStatus(state.currentEvent, status);
        },
        [SET_BLOCK_REMOTE_IMAGES](state, mustBeBlocked) {
            state.remoteImages.mustBeBlocked = mustBeBlocked;
        },
        [RESET_ACTIVE_MESSAGE](state) {
            state.currentEvent = { loading: LoadingStatus.NOT_LOADED };
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

const findEvent = (events, recuridIsoDate) => {
    return events.find(
        event =>
            !recuridIsoDate || event.value.occurrences.some(occurrence => occurrence.recurid.iso8601 === recuridIsoDate)
    );
};
