import { actions, mutations } from "../consultPanel";
import EventHelper from "../helpers/EventHelper";
import inject from "@bluemind/inject";
import { MockCalendarClient } from "@bluemind/test-utils";

const calendarService = new MockCalendarClient();
inject.register({ provide: "CalendarPersistence", factory: () => calendarService });

describe("consultPanel node", () => {
    let context;
    const userUid = "user:uid",
        eventUid = "event:uid",
        newStatus = "ACCEPTED",
        previousStatus = "NO-NO-NO";

    beforeEach(() => {
        context = {
            state: { consultPanel: { currentEvent: {} } },
            getters: {
                CURRENT_MAILBOX: { owner: userUid }
            },
            commit: jest.fn()
        };
    });

    describe("currentEvent", () => {
        test("FETCH_EVENT action", async () => {
            calendarService.getComplete.mockReturnValue("event");
            EventHelper.adapt = jest.fn().mockReturnValue("adaptedEvent");

            await actions.FETCH_EVENT(context, eventUid);
            expect(calendarService.getComplete).toHaveBeenCalledWith(eventUid);
            expect(EventHelper.adapt).toHaveBeenCalledWith("event", userUid);
            expect(context.commit).toHaveBeenCalledWith("SET_CURRENT_EVENT", "adaptedEvent");
        });

        test("SET_EVENT_STATUS action is a success", async () => {
            const serverEventValue = { example: "example" };
            context.state.consultPanel.currentEvent = {
                uid: eventUid,
                status: previousStatus,
                serverEvent: { value: serverEventValue }
            };

            await actions.SET_EVENT_STATUS(context, newStatus);
            expect(context.commit).toHaveBeenCalledWith("SET_CURRENT_EVENT_STATUS", {
                status: newStatus,
                uid: userUid
            });
            expect(calendarService.update).toHaveBeenCalledWith(eventUid, serverEventValue, true);
            expect(context.commit).toHaveBeenCalledTimes(1);
        });

        test("SET_EVENT_STATUS action, optimistic rendering and revert changes if client fails to answer", async () => {
            const serverEventValue = { example: "example" };
            context.state.consultPanel.currentEvent = {
                uid: eventUid,
                status: previousStatus,
                serverEvent: { value: serverEventValue }
            };
            calendarService.update.mockReturnValue(Promise.reject());

            await actions.SET_EVENT_STATUS(context, newStatus);
            expect(context.commit).toHaveBeenNthCalledWith(1, "SET_CURRENT_EVENT_STATUS", {
                status: newStatus,
                uid: userUid
            });
            expect(calendarService.update).toHaveBeenCalledWith(eventUid, serverEventValue, true);
            expect(context.commit).toHaveBeenCalledTimes(2);
            expect(context.commit).toHaveBeenNthCalledWith(2, "SET_CURRENT_EVENT_STATUS", {
                status: previousStatus,
                uid: userUid
            });
        });

        test("SET_CURRENT_EVENT mutation", async () => {
            const event = { example: "example" };
            await mutations.SET_CURRENT_EVENT(context.state, event);
            expect(context.state.consultPanel.currentEvent).toBe(event);
        });

        test("SET_CURRENT_EVENT_STATUS mutation, mutate status but also serverEvent attendee matching my userUid", async () => {
            context.state.consultPanel.currentEvent = {
                status: previousStatus,
                serverEvent: {
                    value: {
                        main: {
                            attendees: [
                                { dir: "bullshit/user/3/dezdez" },
                                { dir: "my/contact/book/3/" + userUid, partStatus: previousStatus }
                            ]
                        }
                    }
                }
            };

            await mutations.SET_CURRENT_EVENT_STATUS(context.state, { status: newStatus, uid: userUid });
            expect(context.state.consultPanel.currentEvent.status).toBe(newStatus);
            expect(context.state.consultPanel.currentEvent.serverEvent.value.main.attendees[1].partStatus).toBe(
                newStatus
            );
        });
    });
});
