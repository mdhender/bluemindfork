import { mount } from "@vue/test-utils";
import EventCanceled from "../EventCanceled.vue";

describe("Event request insert", () => {
    let eventCanceled;

    beforeEach(() => {
        eventCanceled = mount(EventCanceled, {
            propsData: {
                message: {
                    from: { address: "alice@bluemind.net", dn: "Alice" },
                    eventInfo: {}
                }
            },
            mocks: {
                $t: path => path.split(".").pop()
            }
        });
    });

    const getText = () => eventCanceled.find(".event-canceled-text");

    it("is a vue Instance", () => {
        expect(eventCanceled.vm).toBeDefined();
    });
    it("should have the name of the person canceling the event", () => {
        expect(getText().text()).toMatch("Alice");
    });
});
