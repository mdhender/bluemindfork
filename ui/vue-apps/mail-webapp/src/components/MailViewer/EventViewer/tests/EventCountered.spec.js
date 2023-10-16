import { createLocalVue, mount } from "@vue/test-utils";
import EventCountered from "../EventCountered.vue";
import VueI18n from "vue-i18n";
import i18nFiles from "../../../../../l10n";
describe("Event Insert - Fowarded by attendee", () => {
    it("should be a vue component", () => {
        const wrapper = mountEventCountered();
        expect(wrapper.vm).toBeDefined();
    });

    it("shoud have a section with added participants  ", async () => {
        const wrapper = mountEventCountered();
        const addedAttendeesSection = wrapper
            .findAll(".event-footer-section")
            .filter(w => w.text().match("Participants? ajoutés? (\\S)"))
            .at(0);

        expect(addedAttendeesSection.exists()).toBeTruthy();
    });
    it("shoud list new attendee(s) added ", async () => {
        const wrapper = mountEventCountered();
        const listOfAttendees = wrapper
            .findAll(".event-footer-section")
            .filter(w => w.text().match("Participants? ajoutés? (\\S)"))
            .at(0);

        await listOfAttendees.find("button").trigger("click");

        // expect(listOfAttendees.findAll("[role='listitem']").at(0).text()).toMatch("NEW ONE");
        expect(listOfAttendees.findAll("[role='listitem']").at(0).text()).toMatch("<newone@devenv.dev.bluemind.net>");
    });
});

const localVue = createLocalVue();
localVue.use(VueI18n);
const i18n = new VueI18n({
    locale: "fr",
    messages: i18nFiles
});
function mountEventCountered() {
    return mount(EventCountered, {
        localVue,
        i18n,
        propsData: {
            message: {
                headers: [
                    {
                        name: "X-BM-COUNTER-ATTENDEE",
                        values: ["newone@devenv.dev.bluemind.net"]
                    },
                    {
                        name: "X-BM-Event-Countered",
                        values: ['7ae1510f-cfc6-4784-9bad-86c3fbbb81fc; originator="george@devenv.dev.bluemind.net"']
                    }
                ]
            },
            event: {
                attendees: [
                    {
                        name: "George Abitbol",
                        mail: "george@devenv.dev.bluemind.net",
                        status: "NeedsAction",
                        cutype: "Individual"
                    },
                    {
                        name: "NEW ONE",
                        mail: "newone@devenv.dev.bluemind.net",
                        status: "NeedsAction",
                        cutype: "Individual"
                    }
                ]
            }
        }
    });
}
