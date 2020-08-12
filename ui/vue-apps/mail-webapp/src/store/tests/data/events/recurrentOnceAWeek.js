export default {
    value: {
        main: {
            dtstart: { iso8601: "2020-08-21T09:00:00.000+02:00", timezone: "Europe/Paris", precision: "DateTime" },
            summary: "once every week with hour",
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
                frequency: "WEEKLY",
                count: null,
                until: null,
                interval: 1,
                bySecond: [],
                byMinute: [],
                byHour: [],
                byDay: [{ day: "FR", offset: 0 }],
                byMonthDay: [],
                byYearDay: [],
                byWeekNo: [],
                byMonth: []
            },
            url: null,
            attachments: [],
            sequence: 0,
            draft: false,
            dtend: { iso8601: "2020-08-21T10:00:00.000+02:00", timezone: "Europe/Paris", precision: "DateTime" },
            transparency: "Opaque"
        },
        occurrences: [],
        properties: {},
        icsUid: "fecf2e5a-9149-47ec-8768-6aaca6ffa62b"
    },
    uid: "fecf2e5a-9149-47ec-8768-6aaca6ffa62b",
    internalId: 5295,
    version: 31,
    displayName: "once every week with hour",
    externalId: null,
    createdBy: "system",
    updatedBy: "system",
    created: 1597336586337,
    updated: 1597336586337,
    flags: ["Seen"]
};
