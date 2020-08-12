export default {
    value: {
        main: {
            dtstart: { iso8601: "2020-08-25", timezone: null, precision: "Date" },
            summary: "once every week ALL_DAY",
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
            rrule: {
                frequency: "WEEKLY",
                count: null,
                until: null,
                interval: 1,
                bySecond: [],
                byMinute: [],
                byHour: [],
                byDay: [{ day: "TU", offset: 0 }],
                byMonthDay: [],
                byYearDay: [],
                byWeekNo: [],
                byMonth: []
            },
            url: null,
            attachments: [],
            sequence: 0,
            draft: false,
            dtend: { iso8601: "2020-08-26", timezone: null, precision: "Date" },
            transparency: "Opaque"
        },
        occurrences: [],
        properties: {},
        icsUid: "05784243-a2d5-4973-866d-e6ba74b3d0a6"
    },
    uid: "05784243-a2d5-4973-866d-e6ba74b3d0a6",
    internalId: 5300,
    version: 32,
    displayName: "once every week ALL_DAY",
    externalId: null,
    createdBy: "system",
    updatedBy: "system",
    created: 1597336616186,
    updated: 1597336616186,
    flags: ["Seen"]
};
