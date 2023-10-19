import { createLocalVue, mount } from "@vue/test-utils";
import EventCountered from "../EventCountered.vue";
import VueI18n from "vue-i18n";
import i18nFiles from "../../../../../l10n";

const localVue = createLocalVue();
localVue.use(VueI18n);
const i18n = new VueI18n({
    locale: "fr",
    messages: i18nFiles
});
describe("Event Countered", () => {
    describe("Fowarded by attendee", () => {
        it("shoud have a section with added participants  ", async () => {
            const addedAttendeesSection = await extractAttendeesList(mountEventCountered());
            expect(addedAttendeesSection.exists()).toBeTruthy();
        });

        it("shoud list new attendee(s) added ", async () => {
            const listOfAttendees = await extractAttendeesList(mountEventCountered());

            expect(listOfAttendees.findAll("[role='listitem']").at(0).text()).toMatch("NEW ONE");
            expect(listOfAttendees.findAll("[role='listitem']").at(0).text()).toMatch(
                "<newone@devenv.dev.bluemind.net>"
            );
        });

        it("should not show attendees that are not newly added", async () => {
            const listOfAttendees = await extractAttendeesList(mountEventCountered());
            expect(
                findByText(listOfAttendees, {
                    selector: "[role='listitem']",
                    text: "George Abitbol"
                }).exists()
            ).toBeFalsy();
        });

        it("should show numbers of newly added participants", () => {
            expect(
                findByText(mountEventCountered(), {
                    selector: ".event-footer-section",
                    text: "Participant ajouté \\(1\\)"
                }).exists()
            ).toBeTruthy();

            expect(
                findByText(mountEventCountered(["oneMoreAttendee@mail.com"]), {
                    selector: ".event-footer-section",
                    text: "Participants ajoutés \\(2\\)"
                }).exists()
            ).toBeTruthy();
        });
        test("what if no counter attendees value", async () => {
            const wrapper = mount(EventCountered, {
                localVue,
                i18n,
                propsData: {
                    message: {
                        headers: [{ name: "X-BM-COUNTER-ATTENDEE" }]
                    },
                    event: {
                        attendees: []
                    }
                }
            });
            expect(wrapper.find(".event-footer-section").text()).toEqual("Participants ajoutés (0)");
            expect((await extractAttendeesList(wrapper)).find("[role='listitem']").exists()).toBeFalsy();
        });
    });

    async function extractAttendeesList(wrapper) {
        const listOfAttendees = findByText(wrapper, {
            selector: ".event-footer-section",
            text: "Participants? ajoutés? \\(\\d\\)"
        }).at(0);

        await listOfAttendees.find("button").trigger("click");
        return listOfAttendees;
    }
});

function findByText(wrapper, { selector, text }) {
    return wrapper.findAll(selector).filter(w => w.text().match(text));
}

function mountEventCountered(addedAttendees = []) {
    return mount(EventCountered, {
        localVue,
        i18n,
        propsData: {
            message: {
                headers: [
                    {
                        name: "X-BM-COUNTER-ATTENDEE",
                        values: new Array(["newone@devenv.dev.bluemind.net", ...addedAttendees].join(", "))
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
                    },
                    ...addedAttendees.map(aa => ({
                        name: aa.split("@")[0],
                        mail: aa,
                        status: "NeedsAction",
                        cutype: "Individual"
                    }))
                ]
            }
        }
    });
}
