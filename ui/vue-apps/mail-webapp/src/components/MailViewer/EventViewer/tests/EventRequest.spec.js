import { mount } from "@vue/test-utils";
import EventRequest from "../EventRequest.vue";

describe("Event request insert", () => {
    let eventRequest;
    beforeEach(() => {
        eventRequest = mount(EventRequest, {
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
                    }
                },
                message: {
                    eventInfo: {
                        needsReply: true
                    }
                }
            },
            mocks: {
                $t: path => path.split(".").pop()
            }
        });
    });

    const getAcceptButton = () => eventRequest.findAll('[type="button"]').at(0);
    const getMaybeButton = () => eventRequest.findAll('[type="button"]').at(1);
    const getDeclineButton = () => eventRequest.findAll('[type="button"]').at(2);
    const getDetail = () => eventRequest.find(".event-detail");
    const getFooter = () => eventRequest.find(".event-footer");
    const getEventRepliedArg = () => eventRequest.emitted("event-replied")?.[0]?.[0];

    it("is a vue Instance", () => {
        expect(eventRequest.vm).toBeDefined();
    });
    it("should have a button to accept the event", () => {
        expect(getAcceptButton().text()).toEqual("accept");
    });
    it("should have a button to decline the event", () => {
        expect(getDeclineButton().text()).toEqual("decline");
    });
    it("should have a button to answer maybe to the event", () => {
        expect(getMaybeButton().text()).toEqual("tentatively");
    });
    it("should emit `Accepted` when the button accepted is triggered", async () => {
        await getAcceptButton().trigger("click");
        expect(getEventRepliedArg()).toEqual("Accepted");
    });
    it("should emit `tentative` when the button maybe is triggered", async () => {
        await getMaybeButton().trigger("click");
        expect(getEventRepliedArg()).toEqual("Tentative");
    });
    it("should emit `decline` when the button decline is triggered", async () => {
        await getDeclineButton().trigger("click");
        expect(getEventRepliedArg()).toEqual("Declined");
    });
    it("should contain a body", () => {
        expect(getDetail()).toBeDefined();
    });
    it("should contain a footer", () => {
        expect(getFooter()).toBeDefined();
    });
});
