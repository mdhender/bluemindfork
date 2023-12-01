import { mount } from "@vue/test-utils";
import EventDetail from "../base/EventDetail";

describe("Event Insert Body", () => {
    it("should be a Vue component", () => {
        const wrapper = EventDetailComponent().mount();
        expect(wrapper.vm).toBeDefined();
    });

    it("should show title of event", () => {
        const wrapper = EventDetailComponent().mount();
        expect(wrapper.find(".summary").text()).toEqual("SUMMARY TEXT");
    });

    it("should show time of meeting HH:MM - HH:MM", () => {
        const wrapper = EventDetailComponent().mount();
        expect(wrapper.find(".event-time").text()).toEqual("09:00 - 10:00");
    });

    it("time should be cast to local TimeZone if different", () => {
        const wrapper = EventDetailComponent()
            .withEventDate({
                start: "2023-08-14T16:30:00.000+03:00", //new Date(2023, 0, 1, 9, 30),
                end: "2023-08-14T17:30:00.000+03:00" // new Date(2023, 0, 1, 11, 30)
            })
            .mount();

        expect(new Date().getTimezoneOffset()).toBe(0);
        expect(wrapper.find(".event-time").text()).toEqual(`13:30 - 14:30`);
    });

    it("should show occurence of event when there is one", () => {
        const wrapper = EventDetailComponent().mount();
        expect(wrapper.find(".occurence").text()).toEqual(
            "toutes les 2 semaines, le lundi, mardi, mercredi, jeudi et vendredi jusqu’au jeu. 11/08/22"
        );
    });

    it("does not show repetition rule when having a specific occurrence of a serie", () => {
        const wrapper = EventDetailComponent().Occurrence().mount();

        expect(wrapper.find(".occurence").exists()).not.toBeTruthy();
    });
});

function EventDetailComponent(currentEvent) {
    const CURRENT_EVENT = {
        summary: "SUMMARY TEXT",
        date: "toutes les 2 semaines, le lundi, mardi, mercredi, jeudi et vendredi jusqu’au jeu. 11/08/22",
        serverEvent: {
            value: {
                main: {
                    dtstart: { iso8601: new Date(2023, 0, 1, 9, 0).toISOString() },
                    dtend: { iso8601: new Date(2023, 0, 1, 10, 0).toISOString() },
                    rrule: true
                }
            }
        },
        set _dtstart(value) {
            this.serverEvent.value.main.dtstart.iso8601 = new Date(value).toISOString();
        },
        set _dtend(value) {
            this.serverEvent.value.main.dtend.iso8601 = new Date(value).toISOString();
        }
    };

    return {
        withEventDate({ start, end }) {
            CURRENT_EVENT._dtstart = start;
            CURRENT_EVENT._dtend = end;
            return EventDetailComponent(CURRENT_EVENT);
        },
        Occurrence(recuridIsoDate) {
            CURRENT_EVENT.recuridIsoDate = recuridIsoDate ?? new Date(2023, 0, 2, 9, 0).toISOString();
            CURRENT_EVENT.serverEvent.value.occurrences = [];
            return EventDetailComponent(CURRENT_EVENT);
        },
        mount() {
            return mount(EventDetail, {
                propsData: {
                    event: currentEvent ?? CURRENT_EVENT,
                    message: {}
                },
                mocks: {
                    $d: () => {}
                }
            });
        }
    };
}
