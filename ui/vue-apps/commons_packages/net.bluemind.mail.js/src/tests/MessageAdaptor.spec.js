import { MessageBody } from "@bluemind/backend.mail.api";
import { messageUtils } from "@bluemind/mail";
import MessageAdaptor, { getEventInfo } from "../message/MessageAdaptor";

describe("MessageAdaptor", () => {
    const MINIMAL_EMPTY_MESSAGE = { value: { body: { headers: [], recipients: [], structure: { mime: "" } } } };
    const folderRef = { key: "", uid: "" };
    test("message model and message adaptor share same properties", () => {
        const message = messageUtils.create();

        const adapted = MessageAdaptor.fromMailboxItem(MINIMAL_EMPTY_MESSAGE, {});

        expect(Object.keys(message)).toEqual(expect.arrayContaining(Object.keys(adapted)));
    });

    describe("check adaptor results", () => {
        test("remove backslash characters used for escape purpose in recipients (for any kind)", () => {
            const messageWithRecipients = messageBuilder(MINIMAL_EMPTY_MESSAGE)
                .withRecipients(createAllKindOfRecipients("\\John\\ %Un\\\\tel\\% \\(plop\\) \\", "random@mail.com"))
                .build();

            const adapted = MessageAdaptor.fromMailboxItem(messageWithRecipients, folderRef);

            const EXPECTED_DN = "John %Un\\tel% (plop) \\";
            expect(adapted.from.dn).toBe(EXPECTED_DN);
            expect(adapted.to[0].dn).toBe(EXPECTED_DN);
            expect(adapted.cc[0].dn).toBe(EXPECTED_DN);
            expect(adapted.bcc[0].dn).toBe(EXPECTED_DN);
        });

        function createAllKindOfRecipients(dn, address) {
            const recipients = [];
            Object.values(MessageBody.RecipientKind).forEach(kind => {
                recipients.push({
                    dn,
                    address,
                    kind
                });
            });
            return recipients;
        }

        test("Headers values should be decoded when not in utf8", () => {
            const message = messageBuilder(MINIMAL_EMPTY_MESSAGE)
                .withHeader({
                    name: "X-BM-Counter-Attendee",
                    values: ["=?UTF-8?q?Le=C3=AFa_Organa?="]
                })
                .build();

            const Headers_SUT = MessageAdaptor.fromMailboxItem(message, folderRef).headers.find(
                h => h.name === "X-BM-Counter-Attendee"
            );

            expect(Headers_SUT.values).toContain("Leïa Organa");
        });

        function messageBuilder(message = {}) {
            return {
                withRecipients(recipients = []) {
                    message.value.body.recipients = recipients;
                    return messageBuilder(message);
                },
                withHeader(aHeader) {
                    message.value.body.headers.push(aHeader);
                    return messageBuilder(message);
                },
                build() {
                    return message;
                }
            };
        }
    });

    describe("getEventInfo", () => {
        test("simple value", () => {
            const headers = HeaderBuilder().add("X-Bm-Event", "eventId").build();

            const eventInfo = getEventInfo(headers);

            expect(eventInfo).toEqual(
                expect.objectContaining({
                    hasICS: true,
                    isCounterEvent: false,
                    icsUid: "eventId",
                    needsReply: false,
                    recuridIsoDate: null,
                    isResourceBooking: false,
                    resourceUid: ""
                })
            );
        });
        test("when no value, header is ignored", () => {
            const headers = HeaderBuilder().add("X-Bm-Event", "").build();

            const eventInfo = getEventInfo(headers);

            expect(eventInfo).toEqual(
                expect.objectContaining({
                    hasICS: false,
                    isCounterEvent: false,
                    icsUid: "",
                    needsReply: false,
                    recuridIsoDate: null,
                    isResourceBooking: false,
                    resourceUid: "",
                    calendarUid: undefined
                })
            );
        });
        test("with rsvp", () => {
            const headers = HeaderBuilder()
                .add(
                    "X-BM-Event",
                    `eventId;
                             rsvp="true"`
                )
                .build();

            const eventInfo = getEventInfo(headers);

            expect(eventInfo).toEqual(
                expect.objectContaining({
                    hasICS: true,
                    isCounterEvent: false,
                    icsUid: "eventId",
                    needsReply: true,
                    isResourceBooking: false,
                    resourceUid: ""
                })
            );
        });
        test("with recurid", () => {
            const headers = HeaderBuilder()
                .add(
                    "X-BM-Event",
                    `eventId;
                rsvp="true";  recurid="myRecurId"`
                )
                .build();

            const eventInfo = getEventInfo(headers);

            expect(eventInfo).toEqual(
                expect.objectContaining({
                    hasICS: true,
                    isCounterEvent: false,
                    icsUid: "eventId",
                    needsReply: true,
                    recuridIsoDate: "myRecurId",
                    isResourceBooking: false,
                    resourceUid: ""
                })
            );
        });
        test("with counter event", () => {
            const headers = HeaderBuilder()
                .add("X-BM-Event-Countered", `eventId; rsvp="true";  recurid="myRecurId"`)
                .build();

            const eventInfo = getEventInfo(headers);

            expect(eventInfo).toEqual(
                expect.objectContaining({
                    hasICS: true,
                    isCounterEvent: true,
                    icsUid: "eventId",
                    needsReply: true,
                    recuridIsoDate: "myRecurId",
                    isResourceBooking: false,
                    resourceUid: ""
                })
            );
        });
        test("with resource booking", () => {
            const headers = HeaderBuilder()
                .add("X-BM-Event-Countered", `eventId; rsvp="true";  recurid="myRecurId"`)
                .add("X-BM-ResourceBooking", "resourceId")
                .build();

            const eventInfo = getEventInfo(headers);

            expect(eventInfo).toEqual(
                expect.objectContaining({
                    hasICS: true,
                    isCounterEvent: true,
                    icsUid: "eventId",
                    needsReply: true,
                    recuridIsoDate: "myRecurId",
                    isResourceBooking: true,
                    resourceUid: "resourceId"
                })
            );
        });

        function HeaderBuilder(headers = []) {
            return {
                add(headerName, value) {
                    headers.push({ name: headerName, values: [value] });
                    return HeaderBuilder(headers);
                },
                build() {
                    return headers;
                }
            };
        }
    });
});
