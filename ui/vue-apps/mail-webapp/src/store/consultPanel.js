import { Flag } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { loadingStatusUtils, messageUtils } from "@bluemind/mail";
import EventHelper from "./helpers/EventHelper";
import {
    FETCH_EVENT,
    SET_EVENT_STATUS,
    ACCEPT_COUNTER_EVENT,
    DECLINE_COUNTER_EVENT,
    REJECT_ATTENDEES
} from "~/actions";
import {
    RESET_ACTIVE_MESSAGE,
    SET_BLOCK_REMOTE_IMAGES,
    SET_CURRENT_EVENT,
    SET_CURRENT_EVENT_STATUS
} from "~/mutations";
import { Verb } from "@bluemind/core.container.api";
import { cloneDeep } from "lodash";

const { LoadingStatus } = loadingStatusUtils;

export default {
    state: {
        currentEvent: { loading: LoadingStatus.NOT_LOADED },
        remoteImages: { mustBeBlocked: false }
    },
    actions: {
        async [FETCH_EVENT]({ commit }, { message, mailbox }) {
            commit(SET_CURRENT_EVENT, { loading: LoadingStatus.LOADING });
            try {
                let event;
                let calendarUid;
                let calendarOwner;
                if (message.eventInfo.isResourceBooking) {
                    calendarOwner = message.eventInfo.resourceUid;
                    calendarUid = getCalendarUid(calendarOwner, true);
                } else {
                    const otherCalendarUid = (messageUtils.extractHeaderValues(
                        message,
                        messageUtils.MessageHeader.X_BM_CALENDAR
                    ) || [])[0];
                    if (otherCalendarUid) {
                        calendarUid = otherCalendarUid;
                        const otherCalendar = await inject("CalendarsMgmtPersistence").getComplete(otherCalendarUid);
                        if (otherCalendar) {
                            calendarOwner = otherCalendar.owner;
                        }
                    } else {
                        calendarOwner = mailbox.owner;
                        calendarUid = getCalendarUid(calendarOwner);
                    }
                }
                const events = await inject("CalendarPersistence", calendarUid).getByIcsUid(message.eventInfo.icsUid);
                event = EventHelper.findEvent(events, message.eventInfo.recuridIsoDate);
                const mailboxOwner = message.eventInfo.isResourceBooking
                    ? message.eventInfo.resourceUid
                    : mailbox.owner;
                const isWritable =
                    !message.flags.includes(Flag.READ_ONLY_EVENT) && (await isCalendarWritable(calendarUid));
                event = EventHelper.adapt(
                    event,
                    mailboxOwner,
                    message.from.address,
                    message.eventInfo.recuridIsoDate,
                    calendarUid,
                    calendarOwner,
                    isWritable
                );
                commit(SET_CURRENT_EVENT, event);
            } catch {
                commit(SET_CURRENT_EVENT, { loading: LoadingStatus.ERROR });
                throw "Event not found";
            }
        },

        async [REJECT_ATTENDEES]({ commit, state }, { rejectedAttendees }) {
            const updatedEvent = EventHelper.removeAttendees(state.currentEvent, rejectedAttendees);

            await inject("CalendarPersistence", state.currentEvent.calendarUid).update(
                state.currentEvent.uid,
                updatedEvent.serverEvent.value,
                true
            );

            commit(SET_CURRENT_EVENT, {
                ...updatedEvent,
                attendees: EventHelper.adaptAttendeeList(
                    EventHelper.eventInfos(updatedEvent.serverEvent, updatedEvent.recuridIsoDate).attendees
                )
            });
        },

        async [SET_EVENT_STATUS]({ state, commit }, { status }) {
            const previous = { ...state.currentEvent };
            try {
                commit(SET_CURRENT_EVENT_STATUS, { status });
                await inject("CalendarPersistence", previous.calendarUid).update(
                    state.currentEvent.uid,
                    state.currentEvent.serverEvent.value,
                    true
                );
            } catch {
                commit(SET_CURRENT_EVENT_STATUS, { status: previous.status });
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
    const event = cloneDeep(state.currentEvent);

    const recuridIsoDate = event.counter.occurrence ? event.counter.occurrence.recurid.iso8601 : undefined;
    updateFunction(event, event.counter.originator, recuridIsoDate);
    const updatedEvent = EventHelper.adapt(
        event.serverEvent,
        event.mailboxOwner,
        event.counter.originator,
        undefined,
        event.calendarUid,
        event.calendarOwner
    );
    commit(SET_CURRENT_EVENT, updatedEvent);
    await inject("CalendarPersistence", event.calendarUid).update(event.uid, updatedEvent.serverEvent.value, true);
}

function getCalendarUid(owner, isRessource) {
    return isRessource ? `calendar:${owner}` : `calendar:Default:${owner}`;
}

async function isCalendarWritable(calendarUid) {
    const { userId } = inject("UserSession");
    if (calendarUid === getCalendarUid(userId)) {
        return true;
    }
    return inject("ContainerManagementPersistence", calendarUid).canAccess([Verb.Write]);
}
