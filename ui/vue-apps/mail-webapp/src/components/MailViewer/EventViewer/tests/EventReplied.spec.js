import { mount } from "@vue/test-utils";
import EventReplied from "../EventReplied.vue";
import { REPLY_ACTIONS } from "../replyActions";

function mountEventReplied(status) {
    return mount(EventReplied, {
        propsData: {
            currentEvent: {
                loading: "LOADED",
                summary: "SUMMARY TEXT",
                date: "toutes les 2 semaines, le lundi, mardi, mercredi, jeudi et vendredi jusqu’au jeu. 11/08/22",
                status: "?",
                serverEvent: {
                    value: {
                        main: {
                            dtstart: { iso8601: new Date(2023, 0, 1, 9, 0).toISOString() },
                            dtend: { iso8601: new Date(2023, 0, 1, 10, 0).toISOString() }
                        }
                    }
                },
                attendees: [{ name: "Alice", mail: "alice@bluemind.net", status }]
            },
            message: {
                from: { address: "alice@bluemind.net", dn: "Alice" },
                eventInfo: {}
            }
        },
        mocks: {
            $t: path => path.split(".").pop()
        }
    });
}

describe("Event request insert", () => {
    let eventReplied;

    const getHeader = () => eventReplied.find(".event-header");
    const getEventStatus = () => eventReplied.find(".event-replied-status");
    const getDetail = () => eventReplied.find(".event-detail");
    const getFooter = () => eventReplied.find(".event-footer");

    describe("with accepted status", () => {
        beforeEach(() => {
            eventReplied = mountEventReplied(REPLY_ACTIONS.ACCEPTED);
        });

        it("is a vue Instance", () => {
            expect(eventReplied.vm).toBeDefined();
        });
        it("should have a header, a detail and a footer", () => {
            expect(getHeader()).toBeDefined();
            expect(getDetail()).toBeDefined();
            expect(getFooter()).toBeDefined();
        });
        it("should have the name of the person replying to the event", () => {
            expect(getHeader().text()).toMatch("Alice");
        });
        it("should have the status `Accepted` when the person replying to the event has accepted the event", () => {
            expect(getEventStatus().text()).toEqual("accepted");
        });
    });

    describe("with tentative status", () => {
        beforeEach(() => {
            eventReplied = mountEventReplied(REPLY_ACTIONS.TENTATIVE);
        });
        it("should have the status `Tentative` when the person replying to the event has accepted the event", () => {
            expect(getEventStatus().text()).toEqual("tentative");
        });
    });

    describe("with declined status", () => {
        beforeEach(() => {
            eventReplied = mountEventReplied(REPLY_ACTIONS.DECLINED);
        });

        it("should have the status `Declined` when the person replying to the event has accepted the event", () => {
            expect(getEventStatus().text()).toEqual("declined");
        });
    });
});
