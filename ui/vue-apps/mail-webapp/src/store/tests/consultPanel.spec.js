import Vue from "vue";
import Vuex from "vuex";
import cloneDeep from "lodash.clonedeep";
import inject from "@bluemind/inject";
import { MockCalendarClient } from "@bluemind/test-utils";
import storeOptions from "../consultPanel";
import EventHelper from "../helpers/EventHelper";
import { FETCH_EVENT, SET_EVENT_STATUS } from "~/actions";
import { SET_CURRENT_EVENT, SET_CURRENT_EVENT_STATUS, SET_BLOCK_REMOTE_IMAGES } from "~/mutations";

const calendarService = new MockCalendarClient();
inject.register({ provide: "CalendarPersistence", factory: () => calendarService });
Vue.use(Vuex);

describe("consultPanel node", () => {
    const userUid = "user:uid",
        eventUid = "event:uid",
        newStatus = "ACCEPTED",
        previousStatus = "NO-NO-NO",
        serverEvent = {
            value: {
                main: {
                    attendees: [
                        { dir: "my-domain/user/3/dezdez" },
                        { dir: "my/contact/book/3/" + userUid, partStatus: previousStatus }
                    ]
                }
            }
        };
    let store;
    beforeEach(() => {
        store = new Vuex.Store(cloneDeep(storeOptions));
    });

    describe("currentEvent", () => {
        test("FETCH_EVENT action", async () => {
            calendarService.getComplete.mockReturnValue("event");
            EventHelper.adapt = jest.fn().mockReturnValue("adaptedEvent");
            await store.dispatch(FETCH_EVENT, {
                message: { eventInfo: { eventUid }, from: { address: "ori@gina.tor" } },
                mailbox: { owner: userUid }
            });
            expect(calendarService.getComplete).toHaveBeenCalledWith(eventUid);
            expect(EventHelper.adapt).toHaveBeenCalledWith("event", userUid, "ori@gina.tor", undefined);
            expect(store.state.currentEvent).toEqual("adaptedEvent");
        });

        test("FETCH_EVENT action with icsUid", async () => {
            calendarService.getByIcsUid.mockReturnValue(["event"]);
            EventHelper.adapt = jest.fn().mockReturnValue("adaptedEvent");
            await store.dispatch(FETCH_EVENT, {
                message: { eventInfo: { icsUid: "myICS" }, from: { address: "ori@gina.tor" } },
                mailbox: { owner: userUid }
            });
            expect(calendarService.getByIcsUid).toHaveBeenCalledWith("myICS");
            expect(EventHelper.adapt).toHaveBeenCalledWith("event", userUid, "ori@gina.tor", undefined);
            expect(store.state.currentEvent).toEqual("adaptedEvent");
        });

        test("SET_EVENT_STATUS action is a success", async () => {
            store.state.currentEvent = {
                uid: eventUid,
                status: previousStatus,
                serverEvent,
                mailboxOwner: userUid
            };
            const message = { eventInfo: { isResourceBooking: false } };

            await store.dispatch(SET_EVENT_STATUS, { status: newStatus, message });
            expect(calendarService.update).toHaveBeenCalledWith(eventUid, serverEvent.value, true);
            expect(store.state.currentEvent.status).toEqual(newStatus);
        });

        test("SET_EVENT_STATUS action, optimistic rendering and revert changes if client fails to answer", async () => {
            store.state.currentEvent = {
                uid: eventUid,
                status: previousStatus,
                serverEvent,
                mailboxOwner: userUid
            };
            calendarService.update.mockReturnValue(Promise.reject());
            const message = { eventInfo: { isResourceBooking: false } };
            const promise = store.dispatch(SET_EVENT_STATUS, { status: newStatus, message });
            expect(store.state.currentEvent.status).toEqual(newStatus);
            expect(calendarService.update).toHaveBeenCalledWith(eventUid, serverEvent.value, true);
            await promise;
            expect(store.state.currentEvent.status).toEqual(previousStatus);
        });

        test("SET_CURRENT_EVENT mutation", () => {
            const event = { example: "example" };
            storeOptions.mutations[SET_CURRENT_EVENT](store.state, event);
            expect(store.state.currentEvent).toBe(event);
        });

        test("SET_CURRENT_EVENT_STATUS mutation, mutate status but also serverEvent attendee matching my userUid", async () => {
            store.state.currentEvent = {
                status: previousStatus,
                serverEvent,
                mailboxOwner: userUid
            };

            storeOptions.mutations[SET_CURRENT_EVENT_STATUS](store.state, { status: newStatus });
            expect(store.state.currentEvent.status).toBe(newStatus);
            expect(store.state.currentEvent.serverEvent.value.main.attendees[1].partStatus).toBe(newStatus);
        });
    });

    describe("remoteImages", () => {
        test("SET_BLOCK_REMOTE_IMAGES", () => {
            storeOptions.mutations[SET_BLOCK_REMOTE_IMAGES](store.state, true);
            expect(store.state.remoteImages.mustBeBlocked).toBe(true);
        });
    });
});
