export class AttendeeFactory {
    static INDIVIDUAL() {
        return new Attendee({
            commonName: "Leîa Organa",
            cutype: "Individual",
            dir: "bm://75a0d5b3.internal/users/78746D81-D4BE-4F7F-B1F1-7F91D68373A3",
            mailto: "leia@devenv.dev.bluemind.net",
            partStatus: "Tentative",
            role: "RequiredParticipant",
            rsvp: false,
            uri: "addressbook_75a0d5b3.internal/78746D81-D4BE-4F7F-B1F1-7F91D68373A3"
        });
    }

    static RESOURCE() {
        return new Attendee({
            commonName: "ROOM31",
            cutype: "Resource",
            dir: "bm://75a0d5b3.internal/resources/C162C72B-5130-42AD-A545-9600AB95E0XD",
            internal: true,
            mailto: "room31@devenv.dev.bluemind.net",
            partStatus: "NeedsAction",
            role: "RequiredParticipant",
            rsvp: true
        });
    }

    static OWNER() {
        return new Attendee({
            commonName: "The Owner",
            cutype: "Individual",
            dir: "bm://webmail-test.loc/users/C162C72B-5130-42AD-A545-9600AB95E0ED",
            mailto: "the.owner@webmail-test.loc",
            partStatus: "NeedsAction",
            role: "RequiredParticipant",
            rsvp: true
        });
    }
}

class Attendee {
    #value;
    constructor(attendeeDetails) {
        this.#value = {
            counter: null,
            delFrom: null,
            delTo: null,
            internal: true,
            lang: null,
            member: null,
            responseComment: null,
            rsvp: true,
            sentBy: null,
            uri: null,
            ...attendeeDetails
        };
    }
    get Uid() {
        return this.#value.dir.split("/").pop();
    }

    equals(comparator) {
        if (typeof comparator === "string") {
            return this.Uid === comparator;
        }
        return this.comparator.Uid === this.Uid;
    }

    toJson() {
        return {
            ...this.#value
        };
    }
}
