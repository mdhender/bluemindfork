export default {
    value: {
        main: {
            dtstart: { iso8601: "2020-08-14T12:00:00.000+02:00", timezone: "Europe/Paris", precision: "DateTime" },
            summary: "one day, one hour",
            classification: "Public",
            location: null,
            description: null,
            priority: 5,
            alarm: [
                { action: "Display", trigger: -900, description: null, duration: null, repeat: null, summary: null }
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
            dtend: { iso8601: "2020-08-14T13:00:00.000+02:00", timezone: "Europe/Paris", precision: "DateTime" },
            transparency: "Opaque"
        },
        occurrences: [],
        properties: {},
        icsUid: "0f96bbc1-6c50-42ff-b030-8256df315451"
    },
    uid: "0f96bbc1-6c50-42ff-b030-8256df315451",
    internalId: 5280,
    version: 28,
    displayName: "one day, one hour",
    externalId: null,
    createdBy: "system",
    updatedBy: "system",
    created: 1597336498670,
    updated: 1597336498670,
    flags: ["Seen"]
};
