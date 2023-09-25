import { createLocalVue, mount } from "@vue/test-utils";
import VueI18n from "vue-i18n";
import tradFiles from "../../../../../l10n";
import EventCanceled from "../EventCanceled.vue";

describe("Event request insert", () => {
    const localVue = createLocalVue();
    localVue.use(VueI18n);
    const i18nFake = new VueI18n({
        locale: "fr",
        messages: tradFiles
    });
    let eventCanceled;
    beforeEach(() => {
        eventCanceled = mount(EventCanceled, {
            localVue,
            i18n: i18nFake,
            propsData: {
                message: {
                    from: { address: "alice@bluemind.net", dn: "Alice" },
                    eventInfo: {}
                }
            }
        });
    });

    it("is a vue Instance", () => {
        expect(eventCanceled.vm).toBeDefined();
    });
    it("should have the name of the person canceling the event", () => {
        expect(eventCanceled.text()).toEqual("Alice a annulé l'événement.");
    });
});
