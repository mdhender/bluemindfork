import { createLocalVue, mount } from "@vue/test-utils";
import EventNotificationForward from "../EventNotificationForward";
import VueI18n from "vue-i18n";
import i18nFiles from "../../../../../l10n";

const localVue = createLocalVue();
localVue.use(VueI18n);
const i18n = new VueI18n({
    locale: "fr",
    messages: i18nFiles
});
describe("Event Countered - Fowarded by attendee", () => {
    describe("List added attendees", () => {
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
            const wrapper = mount(EventNotificationForward, {
                localVue,
                i18n,
                propsData: {
                    message: {
                        headers: [{ name: "X-BM-COUNTER-ATTENDEE" }]
                    },
                    event: {
                        attendees: [
                            {
                                name: "George Abitbol",
                                mail: "george@devenv.dev.bluemind.net",
                                status: "NeedsAction",
                                cutype: "Individual"
                            }
                        ]
                    }
                }
            });
            expect(wrapper.find(".event-footer-section").text()).toEqual("Participants ajoutés (0)");
            expect((await extractAttendeesList(wrapper)).find("[role='listitem']").exists()).toBeFalsy();
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

    describe("Invitation can be refused by organizer", () => {
        it("should contains a button to refuse new attendee ", () => {
            const wrapper = mountEventCountered();
            const refuseInvitationBtn = findByText(wrapper, {
                selector: "button",
                text: "Refuser"
            }).at(0);

            expect(refuseInvitationBtn.text()).toEqual("Refuser NEW ONE");
        });

        it("reject button should show how many attendees will be rejected when more than one ", () => {
            const wrapper = mountEventCountered(["anymail@gmail.com"]);
            const refuseInvitationBtn = findByText(wrapper, {
                selector: "button",
                text: "Refuser"
            }).at(0);

            expect(refuseInvitationBtn.text()).toEqual("Refuser les 2 participants ajoutés");
        });

        it("when more than five added attendees, button will only propose to rejects them all ", () => {
            const wrapper = mountEventCountered([
                "numero_2_@anymail.com",
                "numero_3_@anymail.com",
                "numero_4_@anymail.com",
                "numero_5_@anymail.com",
                "numero_6_@anymail.com"
            ]);
            const refuseInvitationBtn = findByText(wrapper, {
                selector: "button",
                text: "Refuser"
            }).at(0);

            expect(refuseInvitationBtn.text()).toEqual("Refuser tous les participants ajoutés");
        });

        it("should have a dropdown available when attendees are between 2 and 5", async () => {
            const wrapper = mountEventCountered(["numero1@anymail.com"]);
            const refuseInvitationDropdown = wrapper.find("[role='menu']").findAll('[role="menuitem"]');

            expect(refuseInvitationDropdown.at(0).text()).toEqual("Refuser NEW ONE");
            expect(refuseInvitationDropdown.at(1).text()).toEqual("Refuser numero1");
        });

        it("should be able to decline all at once", async () => {
            const wrapper = mountEventCountered(["numero1@anymail.com"]);
            const refuseInvitationDropdown = wrapper.find("button");

            await refuseInvitationDropdown.trigger("click");

            expect(wrapper.emitted().rejectAttendee[0]).toContainEqual([
                { address: "newone@devenv.dev.bluemind.net", dn: "NEW ONE" },
                { address: "numero1@anymail.com", dn: "numero1" }
            ]);
        });
        it("should be able to decline individually when using dropdown", async () => {
            const wrapper = mountEventCountered(["numero1@anymail.com"]);
            const refuseInvitationDropdown = wrapper.find("[role='menu']").findAll('[role="menuitem"]');

            await refuseInvitationDropdown.at(0).trigger("click");

            expect(wrapper.emitted().rejectAttendee[0]).toContainEqual({
                address: "newone@devenv.dev.bluemind.net",
                dn: "NEW ONE"
            });
        });
    });
});

function findByText(wrapper, { selector, text }) {
    return wrapper.findAll(selector).filter(w => w.text().match(text));
}

function mountEventCountered(addedAttendees = []) {
    return mount(EventNotificationForward, {
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
