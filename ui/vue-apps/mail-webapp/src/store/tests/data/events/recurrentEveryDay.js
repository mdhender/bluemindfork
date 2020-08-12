export default {
    value: {
        main: {
            dtstart: { iso8601: "2020-08-17T12:00:00.000+02:00", timezone: "Europe/Paris", precision: "DateTime" },
            summary: "recurrent : every day with an hour",
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
                frequency: "DAILY",
                count: null,
                until: null,
                interval: 1,
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
            dtend: { iso8601: "2020-08-17T13:30:00.000+02:00", timezone: "Europe/Paris", precision: "DateTime" },
            transparency: "Opaque"
        },
        occurrences: [],
        properties: {},
        icsUid: "69bc4819-b9b6-41ce-bc77-3b648974e953"
    },
    uid: "69bc4819-b9b6-41ce-bc77-3b648974e953",
    internalId: 5285,
    version: 29,
    displayName: "recurrent : every day with an hour",
    externalId: null,
    createdBy: "system",
    updatedBy: "system",
    created: 1597336529234,
    updated: 1597336529234,
    flags: ["Seen"]
};
