import { MessageBodyRecipientKind as RecipientKind } from "@bluemind/backend.mail.api";
import { messageUtils } from "@bluemind/mail";
import MessageAdaptor, { getEventInfo } from "../message/MessageAdaptor";

describe("MessageAdaptor", () => {
    test("message model and message adaptor share same properties", () => {
        const emptyRemote = { value: { body: { headers: [], recipients: [], structure: { mime: "" } } } };

        const message = messageUtils.create();
        const adapted = messageUtils.MessageAdaptor.fromMailboxItem(emptyRemote, {});

        const messageProperties = Object.keys(message).sort();
        const adaptedProperties = Object.keys(adapted).sort();

        expect(messageProperties.length).toBe(adaptedProperties.length);
        messageProperties.forEach((prop, index) => {
            expect(prop).toBe(adaptedProperties[index]);
        });
    });

    describe("check adaptor results", () => {
        let minimalEmptyMessage, folderRef;
        beforeAll(() => {
            minimalEmptyMessage = { value: { body: { headers: [], recipients: [], structure: { mime: "" } } } };
            folderRef = { key: "", uid: "" };
        });
        test("remove backslash characters used for escape purpose in recipients", () => {
            const recipients = [];
            Object.values(RecipientKind).forEach(kind => {
                recipients.push({
                    dn: "\\John\\ %Un\\\\tel\\% \\(plop\\) \\",
                    address: "random@mail.com",
                    kind
                });
            });
            minimalEmptyMessage.value.body.recipients = recipients;
            const adapted = MessageAdaptor.fromMailboxItem(minimalEmptyMessage, folderRef);

            const expected = "John %Un\\tel% (plop) \\";
            expect(adapted.from.dn).toBe(expected);
            expect(adapted.to[0].dn).toBe(expected);
            expect(adapted.cc[0].dn).toBe(expected);
            expect(adapted.bcc[0].dn).toBe(expected);
        });
    });

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