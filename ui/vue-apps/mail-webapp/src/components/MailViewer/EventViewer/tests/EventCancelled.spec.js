import { createLocalVue, mount } from "@vue/test-utils";
import VueI18n from "vue-i18n";
import tradFiles from "../../../../../l10n";
import EventCancelled from "../EventCancelled.vue";

describe("Event request insert", () => {
    const localVue = createLocalVue();
    localVue.use(VueI18n);
    const i18nFake = new VueI18n({
        locale: "fr",
        messages: tradFiles
    });
    let eventCancelled;
    beforeEach(() => {
        eventCancelled = mount(EventCancelled, {
            localVue,
            i18n: i18nFake,
            propsData: {
                message: {
                    from: { address: "alice@bluemind.net", dn: "Alice" },
                    eventInfo: {}
                },
                event: {}
            }
        });
    });

    it("is a vue Instance", () => {
        expect(eventCancelled.vm).toBeDefined();
    });
    it("should have the name of the person canceling the event", () => {
        expect(eventCancelled.text()).toContain("Alice a annulé l'événement.");
    });
});
