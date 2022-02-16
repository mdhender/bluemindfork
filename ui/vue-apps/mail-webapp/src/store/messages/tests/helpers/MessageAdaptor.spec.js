import { getEventInfo } from "../../helpers/MessageAdaptor";

describe("MessageAdaptor", () => {
    describe("getEventInfo", () => {
        const eventId = "eventId";
        test("simple value", () => {
            const headers = [{ name: "X-Bm-Event", values: [eventId] }];
            const eventInfo = getEventInfo(headers);
            expect(eventInfo).toBeTruthy();
            expect(eventInfo.hasICS).toBeTruthy();
            expect(eventInfo.isCounterEvent).toBeFalsy();
            expect(eventInfo.icsUid).toBeTruthy();
            expect(eventInfo.icsUid).toEqual(eventId);
            expect(eventInfo.needsReply).toBeFalsy();
            expect(eventInfo.recuridIsoDate).toBeFalsy();
            expect(eventInfo.isResourceBooking).toBeFalsy();
            expect(eventInfo.resourceUid).toBeFalsy();
        });
        test("no value", () => {
            const headers = [{ name: "X-Bm-Event", values: [""] }];
            const eventInfo = getEventInfo(headers);
            expect(eventInfo).toBeTruthy();
            expect(eventInfo.hasICS).toBeFalsy();
            expect(eventInfo.isCounterEvent).toBeFalsy();
            expect(eventInfo.icsUid).toBeFalsy();
            expect(eventInfo.needsReply).toBeFalsy();
            expect(eventInfo.recuridIsoDate).toBeFalsy();
            expect(eventInfo.isResourceBooking).toBeFalsy();
            expect(eventInfo.resourceUid).toBeFalsy();
        });
        test("with rsvp", () => {
            const headers = [
                {
                    name: "X-BM-Event",
                    values: [
                        `eventId;
                rsvp="true"`
                    ]
                }
            ];
            const eventInfo = getEventInfo(headers);
            expect(eventInfo).toBeTruthy();
            expect(eventInfo.hasICS).toBeTruthy();
            expect(eventInfo.isCounterEvent).toBeFalsy();
            expect(eventInfo.icsUid).toBeTruthy();
            expect(eventInfo.icsUid).toEqual(eventId);
            expect(eventInfo.needsReply).toBeTruthy();
            expect(eventInfo.recuridIsoDate).toBeFalsy();
            expect(eventInfo.isResourceBooking).toBeFalsy();
            expect(eventInfo.resourceUid).toBeFalsy();
        });
        test("with recurid", () => {
            const headers = [
                {
                    name: "X-BM-Event",
                    values: [
                        `eventId;
                rsvp="true";  recurid="myRecurId"`
                    ]
                }
            ];
            const eventInfo = getEventInfo(headers);
            expect(eventInfo).toBeTruthy();
            expect(eventInfo.hasICS).toBeTruthy();
            expect(eventInfo.isCounterEvent).toBeFalsy();
            expect(eventInfo.icsUid).toBeTruthy();
            expect(eventInfo.icsUid).toEqual(eventId);
            expect(eventInfo.needsReply).toBeTruthy();
            expect(eventInfo.recuridIsoDate).toBeTruthy();
            expect(eventInfo.isResourceBooking).toBeFalsy();
            expect(eventInfo.resourceUid).toBeFalsy();
        });
        test("with counter event", () => {
            const headers = [
                {
                    name: "X-BM-Event-Countered",
                    values: [
                        `eventId;
                rsvp="true";  recurid="myRecurId"`
                    ]
                }
            ];
            const eventInfo = getEventInfo(headers);
            expect(eventInfo).toBeTruthy();
            expect(eventInfo.hasICS).toBeTruthy();
            expect(eventInfo.isCounterEvent).toBeTruthy();
            expect(eventInfo.icsUid).toBeTruthy();
            expect(eventInfo.icsUid).toEqual(eventId);
            expect(eventInfo.needsReply).toBeTruthy();
            expect(eventInfo.recuridIsoDate).toBeTruthy();
            expect(eventInfo.isResourceBooking).toBeFalsy();
            expect(eventInfo.resourceUid).toBeFalsy();
        });
        test("with resource booking", () => {
            const headers = [
                {
                    name: "X-BM-Event-Countered",
                    values: [
                        `eventId;
                rsvp="true";  recurid="myRecurId"`
                    ]
                },
                { name: "X-BM-ResourceBooking", values: ["resourceId"] }
            ];
            const eventInfo = getEventInfo(headers);
            expect(eventInfo).toBeTruthy();
            expect(eventInfo.hasICS).toBeTruthy();
            expect(eventInfo.isCounterEvent).toBeTruthy();
            expect(eventInfo.icsUid).toBeTruthy();
            expect(eventInfo.icsUid).toEqual(eventId);
            expect(eventInfo.needsReply).toBeTruthy();
            expect(eventInfo.recuridIsoDate).toBeTruthy();
            expect(eventInfo.isResourceBooking).toBeTruthy();
            expect(eventInfo.resourceUid).toBeTruthy();
            expect(eventInfo.resourceUid).toEqual("resourceId");
        });
    });
});
