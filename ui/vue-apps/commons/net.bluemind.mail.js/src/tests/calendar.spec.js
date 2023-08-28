import { hasCalendarPart } from "../message/structureParsers";

describe("CALENDAR utils", () => {
    describe("hasCalendar", () => {
        test("only textPlain", () => {
            const textPlain = { mime: "text/plain" };
            expect(hasCalendarPart(textPlain)).toBeFalsy();
        });

        test("simple structure", () => {
            const firstLevelTextCalendar = {
                mime: "text/calendar",
                address: "d000c30f-ec16-4f08-b7a9-e10ee6b60eae",
                encoding: "quoted-printable",
                charset: "utf-8"
            };
            expect(hasCalendarPart(firstLevelTextCalendar)).toBeTruthy();
        });

        test("text/calendar is nested", () => {
            const nestedStructure = {
                mime: "multipart/mixed",
                children: [
                    {
                        mime: "multipart/alternative",
                        children: [
                            { mime: "text/plain", address: null, encoding: "quoted-printable", charset: "utf-8" },
                            { mime: "text/html", address: null, encoding: "quoted-printable", charset: "utf-8" },
                            {
                                mime: "text/calendar",
                                address: "d000c30f-ec16-4f08-b7a9-e10ee6b60eae",
                                encoding: "quoted-printable",
                                charset: "utf-8"
                            }
                        ]
                    },
                    {
                        address: "698577c1-f9e5-4afd-b473-6ac546831098",
                        charset: "us-ascii",
                        dispositionType: "ATTACHMENT",
                        encoding: "base64",
                        fileName: "event.ics",
                        mime: "application/ics",
                        size: 1331
                    }
                ]
            };

            expect(hasCalendarPart(nestedStructure)).toBeTruthy();
        });
    });
});
