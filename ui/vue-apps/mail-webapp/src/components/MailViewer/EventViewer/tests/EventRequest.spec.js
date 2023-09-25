import { mount } from "@vue/test-utils";
import EventRequest from "../EventRequest.vue";

import store from "@bluemind/store";
jest.mock("@bluemind/store", () => ({
    dispatch: jest.fn(),
    hasModule: () => true,
    _reset() {
        this.dispatch = jest.fn();
    }
}));

describe("Event request insert", () => {
    let eventRequest;

    beforeEach(() => {
        eventRequest = mount(EventRequest, {
            propsData: {
                event: {
                    isWritable: true,
                    status: "NO STATUS YET"
                },
                message: {
                    eventInfo: {
                        needsReply: true
                    }
                }
            },
            mocks: {
                $t: path => path.split(".").pop(),
                $tc: path => path.split(".").pop()
            }
        });
    });

    afterEach(() => {
        store._reset();
    });

    const getAcceptButton = () => eventRequest.findAll('[type="button"]').at(0);
    const getMaybeButton = () => eventRequest.findAll('[type="button"]').at(1);
    const getDeclineButton = () => eventRequest.findAll('[type="button"]').at(2);
    const getDetail = () => eventRequest.find(".event-detail");
    const getFooter = () => eventRequest.find(".event-footer");

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

        expect(store.dispatch).toHaveBeenCalledTimes(1);
        expect(store.dispatch.mock.calls[0][1]).toMatchObject({ status: "Accepted" });
    });
    it("should emit `tentative` when the button maybe is triggered", async () => {
        await getMaybeButton().trigger("click");

        expect(store.dispatch).toHaveBeenCalledTimes(1);
        expect(store.dispatch.mock.calls[0][1]).toMatchObject({ status: "Tentative" });
    });
    it("should emit `decline` when the button decline is triggered", async () => {
        await getDeclineButton().trigger("click");

        expect(store.dispatch).toHaveBeenCalledTimes(1);
        expect(store.dispatch.mock.calls[0][1]).toMatchObject({ status: "Declined" });
    });
    it("should contain a body", () => {
        expect(getDetail()).toBeDefined();
    });
    it("should contain a footer", () => {
        expect(getFooter()).toBeDefined();
    });
    it("should not have reply buttons when the event is not writable", () => {
        const eventRequestUnwritable = mount(EventRequest, {
            propsData: {
                event: {
                    isWritable: false,
                    status: "NO STATUS YET"
                },
                message: {}
            },
            mocks: {
                $t: path => path.split(".").pop(),
                $tc: path => path.split(".").pop()
            }
        });
        expect(eventRequestUnwritable.findAll('[type="button"]').length === 0);
    });
});
