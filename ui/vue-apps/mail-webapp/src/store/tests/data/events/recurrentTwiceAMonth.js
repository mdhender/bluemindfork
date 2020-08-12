export default {
    value: {
        main: {
            dtstart: { iso8601: "2020-08-19T09:00:00.000+02:00", timezone: "Europe/Paris", precision: "DateTime" },
            summary: "twice a month, with an hour",
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
            rrule: {
                frequency: "MONTHLY",
                count: null,
                until: null,
                interval: 2,
                bySecond: [],
                byMinute: [],
                byHour: [],
                byDay: [],
                byMonthDay: [],
                byYearDay: [],
                byWeekNo: [],
                byMonth: []
            },
            url: null,
            attachments: [],
            sequence: 0,
            draft: false,
            dtend: { iso8601: "2020-08-19T09:30:00.000+02:00", timezone: "Europe/Paris", precision: "DateTime" },
            transparency: "Opaque"
        },
        occurrences: [],
        properties: {},
        icsUid: "41a4e2e2-8eef-449c-9631-812727514880"
    },
    uid: "41a4e2e2-8eef-449c-9631-812727514880",
    internalId: 5290,
    version: 30,
    displayName: "twice a month, with an hour",
    externalId: null,
    createdBy: "system",
    updatedBy: "system",
    created: 1597336556578,
    updated: 1597336556578,
    flags: ["Seen"]
};
