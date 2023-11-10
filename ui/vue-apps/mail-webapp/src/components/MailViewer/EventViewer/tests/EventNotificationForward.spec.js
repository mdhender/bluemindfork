import { createLocalVue, mount } from "@vue/test-utils";
import EventNotificationForward from "../EventNotificationForward";
import VueI18n from "vue-i18n";
import i18nFiles from "../../../../../l10n";
import inject from "@bluemind/inject";
import consultPanel from "~/store/consultPanel";
import Vuex from "vuex";
import store from "@bluemind/store";
import EventHelper from "../../../../store/helpers/EventHelper";

jest.mock("@bluemind/webappdata");
inject.register({ provide: "CalendarPersistence", factory: () => ({ update: () => true }) });
inject.register({
    provide: "AddressBooksPersistence",
    factory: () => ({
        search: vCard => {
            const mails = vCard.query.split(" ")[0].split("value:(")[1].split(")")[0].split(",");
            return {
                total: mails.length,
                values: mails.map(mail => ({ value: { mail: mail, formatedName: mail.split("@")[0] } }))
            };
        }
    })
});

const localVue = createLocalVue();
localVue.use(VueI18n);
localVue.use(Vuex);
const i18n = new VueI18n({
    locale: "fr",
    messages: i18nFiles
});
describe("Event Countered - Fowarded by attendee", () => {
    beforeAll(() => {
        if (!store.hasModule("mail")) {
            store.registerModule("mail", { namespaced: true });
        }
        if (store.hasModule("mail") && !store.hasModule("consultPanel")) {
            store.registerModule(["mail", "consultPanel"], consultPanel);
        }
    });
    afterAll(() => {
        if (store.hasModule("mail")) {
            store.unregisterModule("mail");
        }
    });
    afterEach(() => {
        jest.resetAllMocks();
    });
    describe("List added attendees", () => {
        it("shoud have a section with added participants  ", async () => {
            const addedAttendeesSection = await extractAttendeesList(mountEventCountered());
            expect(addedAttendeesSection.exists()).toBeTruthy();
        });

        it("shoud list new attendee(s) added ", async () => {
            const listOfAttendees = await extractAttendeesList(mountEventCountered());

            expect(listOfAttendees.findAll("[role='listitem']").at(0).text()).toMatch(
                /NEW ONE\s*<newone@devenv.dev.bluemind.net>/
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

        it("should not show list if all added attendees have been rejected", () => {
            const wrapper = mount(EventNotificationForward, {
                localVue,
                i18n,
                propsData: {
                    message: {
                        headers: [
                            {
                                name: "X-BM-COUNTER-ATTENDEE",
                                values: ['"NEW ONE" <newone@devenv.dev.bluemind.net>']
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
                            }
                        ]
                    }
                }
            });

            expect(
                findByText(wrapper, {
                    selector: ".event-footer-section",
                    text: "Participants? ajoutés? \\(\\d\\)"
                }).exists()
            ).toBeFalsy();
        });
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
            const wrapper = mountEventCountered(["numero1@anymail.com"], { requireStore: true });
            const refuseInvitationDropdown = wrapper.find("button");

            await refuseInvitationDropdown.trigger("click");
            /** WE must simulate the update of props since it is managed by the parent */
            await wrapper.setProps({ event: store.state.mail.consultPanel.currentEvent });

            expect(
                findByText(wrapper, {
                    selector: ".event-footer-section",
                    text: "Participants? ajoutés? \\(\\d\\)"
                }).exists()
            ).toBeFalsy();
        });
        it("should be able to decline individually when using dropdown", async () => {
            const wrapper = mountEventCountered(["numero1@anymail.com"], { requireStore: true });
            const refuseInvitationDropdown = wrapper.find("[role='menu']").findAll('[role="menuitem"]');

            await refuseInvitationDropdown.at(0).trigger("click");
            await wrapper.setProps({ event: store.state.mail.consultPanel.currentEvent });
            const attendeesToAdd = await extractAttendeesList(wrapper);

            expect(attendeesToAdd.text()).toMatch("numero1");
            expect(attendeesToAdd.text()).not.toMatch("NEW ONE");
        });

        test("when attendees have been rejected, a section below 'added participants' should be visible", async () => {
            const wrapper = mountEventCountered([], {
                alreadyRejected: ["newone@devenv.dev.bluemind.net"]
            });

            expect(
                findByText(wrapper, {
                    selector: ".event-footer-section",
                    text: "Participants? refusés? \\(1\\)"
                }).exists()
            ).toBeTruthy();
        });

        it("should display each rejected attendees in related section within footer", async () => {
            const wrapper = mountEventCountered(['"AN OTHER" <another@devenv.dev.bluemind.net>'], {
                alreadyRejected: ["newone@devenv.dev.bluemind.net"]
            });

            const rejectedAttendeesList = findByText(wrapper, {
                selector: ".event-footer-section",
                text: "Participants? refusés? \\(\\d\\)"
            }).at(0);
            await rejectedAttendeesList.find("button").trigger("click");

            expect(rejectedAttendeesList.findAll('[role="listitem"]').at(0).text()).toMatch(
                /NEW ONE\s*<newone@devenv.dev.bluemind.net>/
            );
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

    describe("When event is an exception (aka one occurence of a serie)", () => {
        it("should reject attendee only from current occurrence", async () => {
            const wrapper = mount(EventNotificationForward, {
                localVue,
                i18n,
                propsData: {
                    message: {
                        eventInfos: {},
                        headers: [
                            {
                                name: "X-BM-COUNTER-ATTENDEE",
                                values: new Array(['"TEST max" <max@devenv.dev.bluemind.net>'].join(", "))
                            },
                            {
                                name: "X-BM-Event-Countered",
                                values: [
                                    '7771e20a-be32-48c8-8d09-95b23ddcd510; originator="george@devenv.dev.bluemind.net"; recurid="2023-11-16T09:00:00.000+01:00"'
                                ]
                            }
                        ]
                    },
                    event: occurenceForwarded
                },
                mocks: {
                    $t: path => (path === "styleguide.contact-input.invalid" ? path : i18n.t(path)),
                    $d: path => path
                },
                attachTo: document.body
            });
            store.commit("mail/SET_CURRENT_EVENT", occurenceForwarded);

            const refuseInvitationDropdown = wrapper.find("button");
            await refuseInvitationDropdown.trigger("click");

            expect(
                EventHelper.eventInfos(
                    store.state.mail.consultPanel.currentEvent.serverEvent,
                    store.state.mail.consultPanel.currentEvent.recuridIsoDate
                ).attendees.map(attendee => ({
                    name: attendee.commonName,
                    mail: attendee.mailto,
                    status: attendee.partStatus,
                    cutype: attendee.cutype
                }))
            ).toEqual(store.state.mail.consultPanel.currentEvent.attendees);
        });

        const serverEvent = {
            value: {
                main: {
                    summary: "EVERYDAY MEETING",
                    attendees: [
                        {
                            cutype: "Individual",
                            role: "RequiredParticipant",
                            partStatus: "Tentative",
                            rsvp: false,
                            commonName: "George Abitbol",
                            dir: "bm://75a0d5b3.internal/users/A574855D-907D-4B73-83C3-E2F1D794B50F",
                            mailto: "george@devenv.dev.bluemind.net",
                            uri: "addressbook_75a0d5b3.internal/A574855D-907D-4B73-83C3-E2F1D794B50F"
                        }
                    ],
                    organizer: {
                        commonName: "Jean Giono",
                        mailto: "j.giono@devenv.dev.bluemind.net",
                        dir: "bm://75a0d5b3.internal/users/79E5C4EB-060F-46CB-88F9-F218E7F139F7"
                    },
                    rrule: {
                        frequency: "DAILY",
                        count: 10,
                        interval: 1
                    },
                    dtend: {
                        iso8601: "2023-11-13T09:30:00.000+01:00",
                        timezone: "Europe/Paris",
                        precision: "DateTime"
                    },
                    transparency: "Opaque"
                },
                occurrences: [
                    {
                        attendees: [
                            {
                                cutype: "Individual",
                                role: "RequiredParticipant",
                                partStatus: "NeedsAction",
                                rsvp: true,
                                commonName: "George Abitbol",
                                dir: "bm://75a0d5b3.internal/users/A574855D-907D-4B73-83C3-E2F1D794B50F",
                                mailto: "george@devenv.dev.bluemind.net",
                                uri: "addressbook_75a0d5b3.internal/A574855D-907D-4B73-83C3-E2F1D794B50F"
                            }
                        ],
                        recurid: {
                            iso8601: "2023-11-15T09:00:00.000+01:00",
                            timezone: "Europe/Paris",
                            precision: "DateTime"
                        }
                    },
                    {
                        dtstart: {
                            iso8601: "2023-11-16T09:00:00.000+01:00",
                            timezone: "Europe/Paris",
                            precision: "DateTime"
                        },
                        attendees: [
                            {
                                cutype: "Individual",
                                role: "RequiredParticipant",
                                partStatus: "Accepted",
                                rsvp: false,
                                commonName: "George Abitbol",
                                dir: "bm://75a0d5b3.internal/users/A574855D-907D-4B73-83C3-E2F1D794B50F",
                                mailto: "george@devenv.dev.bluemind.net",
                                uri: "addressbook_75a0d5b3.internal/A574855D-907D-4B73-83C3-E2F1D794B50F"
                            },
                            {
                                cutype: "Individual",
                                role: "OptionalParticipant",
                                partStatus: "NeedsAction",
                                commonName: "TEST max",
                                dir: "bm://75a0d5b3.internal/users/6A3A9AF9-7073-4D49-B6F1-688D710ADA5E",
                                mailto: "max@devenv.dev.bluemind.net"
                            }
                        ],
                        // rrule: {
                        //     frequency: "DAILY",
                        //     count: 10,
                        //     interval: 1
                        // },

                        recurid: {
                            iso8601: "2023-11-16T09:00:00.000+01:00",
                            timezone: "Europe/Paris",
                            precision: "DateTime"
                        }
                    }
                ]
            }
        };

        const occurenceForwarded = {
            summary: "EVERYDAY MEETING",
            organizer: { name: "Jean Giono", mail: "j.giono@devenv.dev.bluemind.net" },
            date: "Tous les jours, de 09:00 à 09:30, à partir du 16 novembre 2023",
            attendees: [
                {
                    name: "George Abitbol",
                    mail: "george@devenv.dev.bluemind.net",
                    status: "Accepted",
                    cutype: "Individual"
                },
                { name: "TEST max", mail: "max@devenv.dev.bluemind.net", status: "NeedsAction", cutype: "Individual" }
            ],
            recuridIsoDate: "2023-11-16T09:00:00.000+01:00",
            serverEvent: serverEvent,
            loading: "LOADED"
        };
    });
});

function findByText(wrapper, { selector, text }) {
    return wrapper.findAll(selector).filter(w => w.text().match(text));
}

function mountEventCountered(addedAttendees = [], options = { requireStore: false, alreadyRejected: [] }) {
    if (!("alreadyRejected" in options)) options.alreadyRejected = [];
    if (!("requireStore" in options)) options.requireStore = false;

    const wrapper = mount(EventNotificationForward, {
        localVue,
        i18n,
        propsData: {
            message: {
                headers: [
                    {
                        name: "X-BM-COUNTER-ATTENDEE",
                        values: new Array(
                            [
                                '"NEW ONE" <newone@devenv.dev.bluemind.net>',
                                ...addedAttendees.map(aa => aa.split("@")[0] + " <" + aa + ">")
                            ].join(", ")
                        )
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
                ].filter(aa => !options.alreadyRejected.includes(aa.mail))
            }
            /**
             * serverEvent.value.main.attendees is omitted in the setup
             * but it is actually required to handle actions of rejecting attendee
             * outside of Test context
             * */
        },
        mocks: {
            $t: path => (path === "styleguide.contact-input.invalid" ? path : i18n.t(path)),
            $d: path => path
        },
        attachTo: document.body
    });

    if (options.requireStore) {
        store.commit("mail/SET_CURRENT_EVENT", {
            serverEvent: {
                value: {
                    main: {
                        dtstart: { iso8601: new Date(2023, 0, 1, 9, 0).toISOString() },
                        attendees: [
                            {
                                commonName: "George Abitbol",
                                mailto: "george@devenv.dev.bluemind.net",
                                status: "NeedsAction",
                                cutype: "Individual"
                            },
                            {
                                commonName: "NEW ONE",
                                mailto: "newone@devenv.dev.bluemind.net",
                                status: "NeedsAction",
                                cutype: "Individual"
                            },
                            ...addedAttendees.map(aa => ({
                                commonName: aa.split("@")[0],
                                mailto: aa,
                                status: "NeedsAction",
                                cutype: "Individual"
                            }))
                        ]
                    }
                }
            }
        });
    }
    return wrapper;
}
