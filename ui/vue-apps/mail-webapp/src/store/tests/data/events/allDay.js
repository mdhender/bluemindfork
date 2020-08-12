export default {
    value: {
        main: {
            dtstart: { iso8601: "2020-08-14", timezone: null, precision: "Date" },
            summary: "all day event",
            classification: "Public",
            location: null,
            description: null,
            priority: 5,
            alarm: [
                { action: "Display", trigger: -25200, description: null, duration: null, repeat: null, summary: null }
            ],
            status: "Confirmed",
            attendees: [
                {
                    cutype: "Individual",
                    member: null,
                    role: "RequiredParticipant",
                    partStatus: "NeedsAction",
                    rsvp: true,
                    delTo: null,
                    delFrom: null,
                    sentBy: null,
                    commonName: "test",
                    dir: "bm://webmail-test.loc/users/B2CBEEFD-147C-451A-9229-1B6C9697D202",
                    lang: null,
                    mailto: "test@webmail-test.loc",
                    uri: null,
                    internal: true,
                    responseComment: null
                }
            ],
            organizer: {
                uri: null,
                commonName: "john doe",
                mailto: "jdoe@webmail-test.loc",
                dir: "bm://webmail-test.loc/users/6926E3AD-6CA1-4147-875F-85F080E64AFA"
            },
            categories: [],
            exdate: null,
            rdate: null,
            rrule: null,
            url: null,
            attachments: [],
            sequence: 0,
            draft: false,
            dtend: { iso8601: "2020-08-15", timezone: null, precision: "Date" },
            transparency: "Transparent"
        },
        occurrences: [],
        properties: {},
        icsUid: "191319a5-07b8-40bc-80b4-aeae35951029"
    },
    uid: "191319a5-07b8-40bc-80b4-aeae35951029",
    internalId: 5276,
    version: 27,
    displayName: "all day event",
    externalId: null,
    createdBy: "system",
    updatedBy: "system",
    created: 1597336484841,
    updated: 1597336484841,
    flags: ["Seen"]
};
