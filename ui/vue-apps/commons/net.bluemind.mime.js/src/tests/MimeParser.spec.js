import fs from "fs";
import path from "path";
import MimeParser from "../MimeParser";

describe("MimeParser", () => {
    describe("structure", () => {
        test("basic_text", async () => {
            const eml = readEml("basic_text");
            const parser = await new MimeParser().parse(eml);
            expect(parser.structure).toMatchInlineSnapshot(`
                Object {
                  "address": "1",
                  "charset": "utf-8",
                  "dispositionType": "INLINE",
                  "encoding": "quoted-printable",
                  "mime": "text/plain",
                  "size": 19,
                }
            `);
        });
        test("basic_html", async () => {
            const eml = readEml("basic_html");
            const parser = await new MimeParser().parse(eml);
            expect(parser.structure).toMatchInlineSnapshot(`
                Object {
                  "address": "1",
                  "charset": "utf-8",
                  "dispositionType": "INLINE",
                  "encoding": "quoted-printable",
                  "mime": "text/html",
                  "size": 118,
                }
            `);
        });
        test("multi_alternative", async () => {
            const eml = readEml("multi_alternative");
            const parser = await new MimeParser().parse(eml);
            expect(parser.structure).toMatchInlineSnapshot(`
                Object {
                  "address": "TEXT",
                  "children": Array [
                    Object {
                      "address": "1",
                      "charset": "utf-8",
                      "dispositionType": "INLINE",
                      "encoding": "quoted-printable",
                      "mime": "text/plain",
                      "size": 45,
                    },
                    Object {
                      "address": "2",
                      "charset": "utf-8",
                      "dispositionType": "INLINE",
                      "encoding": "quoted-printable",
                      "mime": "text/html",
                      "size": 126,
                    },
                  ],
                  "mime": "multipart/alternative",
                }
            `);
        });
        test("multi_related", async () => {
            const eml = readEml("multi_related");
            const parser = await new MimeParser().parse(eml);
            expect(parser.structure).toMatchInlineSnapshot(`
                Object {
                  "address": "TEXT",
                  "children": Array [
                    Object {
                      "address": "1",
                      "charset": "utf-8",
                      "dispositionType": "INLINE",
                      "encoding": "quoted-printable",
                      "mime": "text/html",
                      "size": 223,
                    },
                    Object {
                      "address": "2",
                      "charset": "us-ascii",
                      "contentId": "<0d196b557d8acd1f1f81a9f96a331f4c@bluemind.net>",
                      "dispositionType": "INLINE",
                      "encoding": "base64",
                      "fileName": "smile.png",
                      "mime": "image/png",
                      "size": 1738,
                    },
                  ],
                  "mime": "multipart/related",
                }
            `);
        });
        test("multi_mixed", async () => {
            const eml = readEml("multi_mixed");
            const parser = await new MimeParser().parse(eml);
            expect(parser.structure).toMatchInlineSnapshot(`
                Object {
                  "address": "TEXT",
                  "children": Array [
                    Object {
                      "address": "1",
                      "charset": "utf-8",
                      "dispositionType": "INLINE",
                      "encoding": "quoted-printable",
                      "mime": "text/plain",
                      "size": 22,
                    },
                    Object {
                      "address": "2",
                      "charset": "us-ascii",
                      "contentId": undefined,
                      "dispositionType": "ATTACHMENT",
                      "encoding": "base64",
                      "fileName": "smile.png",
                      "mime": "image/png",
                      "size": 1738,
                    },
                  ],
                  "mime": "multipart/mixed",
                }
            `);
        });
        test("multi_full", async () => {
            const eml = readEml("multi_full");
            const parser = await new MimeParser().parse(eml);
            expect(parser.structure).toMatchInlineSnapshot(`
                Object {
                  "address": "TEXT",
                  "children": Array [
                    Object {
                      "address": "1",
                      "children": Array [
                        Object {
                          "address": "1.1",
                          "charset": "utf-8",
                          "dispositionType": "INLINE",
                          "encoding": "quoted-printable",
                          "mime": "text/plain",
                          "size": 24,
                        },
                        Object {
                          "address": "1.2",
                          "children": Array [
                            Object {
                              "address": "1.2.1",
                              "charset": "utf-8",
                              "dispositionType": "INLINE",
                              "encoding": "quoted-printable",
                              "mime": "text/html",
                              "size": 322,
                            },
                            Object {
                              "address": "1.2.2",
                              "charset": "us-ascii",
                              "contentId": "<a0fdd7347c653e453159dd85588d6ad6@bluemind.net>",
                              "dispositionType": "INLINE",
                              "encoding": "base64",
                              "fileName": "image-a0fdd.png",
                              "mime": "image/png",
                              "size": 1738,
                            },
                          ],
                          "mime": "multipart/related",
                        },
                      ],
                      "mime": "multipart/alternative",
                    },
                    Object {
                      "address": "2",
                      "charset": "us-ascii",
                      "contentId": undefined,
                      "dispositionType": "ATTACHMENT",
                      "encoding": "base64",
                      "fileName": "smile.png",
                      "mime": "image/png",
                      "size": 1738,
                    },
                  ],
                  "mime": "multipart/mixed",
                }
            `);
        });
        test("change base address", async () => {
            let eml, parser;
            eml = readEml("basic_text");
            parser = await new MimeParser("1.2").parse(eml);
            expect(parser.structure.address).toBe("1.2");
            eml = readEml("multi_mixed");
            parser = await new MimeParser("1").parse(eml);
            expect(parser.structure.address).toBe("1");
            expect(parser.structure.children[0].address).toBe("1.1");
            expect(parser.structure.children[1].address).toBe("1.2");
            parser = await new MimeParser("2.1").parse(eml);
            expect(parser.structure.address).toBe("2.1");
            expect(parser.structure.children[0].address).toBe("2.1.1");
            expect(parser.structure.children[1].address).toBe("2.1.2");
        });
    });
    describe("hasAttachment", () => {
        test("to be true if part contain attachments", async () => {
            const eml = readEml("multi_mixed");
            const parser = await new MimeParser().parse(eml);
            expect(parser.hasAttachment()).toBeTruthy();
        });
        test("to be false if part only contain inline parts", async () => {
            const eml = readEml("basic_text");
            const parser = await new MimeParser().parse(eml);
            expect(parser.hasAttachment()).toBeFalsy();
        });
        test("to be false if part only contains inline and related parts", async () => {
            const eml = readEml("multi_related");
            const parser = await new MimeParser().parse(eml);
            expect(parser.hasAttachment()).toBeFalsy();
        });
    });
    describe("getPartContent", () => {
        test("return text content", async () => {
            const eml = readEml("basic_text");
            const parser = await new MimeParser().parse(eml);
            const content = parser.getPartContent("1");
            expect(content instanceof ArrayBuffer).toBeTruthy();
            expect(content.byteLength).toBe(19);
        });
        test("return attachment content", async () => {
            const eml = readEml("multi_mixed");
            const parser = await new MimeParser().parse(eml);
            const content = parser.getPartContent("2");
            expect(content instanceof ArrayBuffer).toBeTruthy();
            expect(content.byteLength).toBe(1738);
        });
        test("return empty if no content", async () => {
            const eml = readEml("basic_text");
            const parser = await new MimeParser().parse(eml);
            expect(parser.getPartContent("1.2")).toBeUndefined();
        });
    });
});

function readEml(file) {
    return fs.readFileSync(path.join(__dirname, `./datas/eml/${file}.eml`), "utf8", (err, data) => data);
}
