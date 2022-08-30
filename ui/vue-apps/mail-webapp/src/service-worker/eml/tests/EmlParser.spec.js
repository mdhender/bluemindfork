import fs from "fs";
import path from "path";
import EmlParser from "../EmlParser";

const body = { bodyVersion: 0, date: 0, guid: 0 };

describe("EmlParser", () => {
    describe("parseBodyStructure", () => {
        test("basic_text", async () => {
            const eml = readEml("basic_text");
            const structure = await EmlParser.parseBodyStructure(body, eml);
            expect(structure).toMatchInlineSnapshot(`
                Object {
                  "bodyVersion": 0,
                  "date": 0,
                  "guid": 0,
                  "preview": "[Decrypted message] Basic Text content",
                  "smartAttach": false,
                  "structure": Object {
                    "address": "1",
                    "dispositionType": "INLINE",
                    "encoding": "",
                    "mime": "text/plain",
                    "size": 19,
                  },
                }
            `);
        });
        test("basic_html", async () => {
            const eml = readEml("basic_html");
            const structure = await EmlParser.parseBodyStructure(body, eml);
            expect(structure).toMatchInlineSnapshot(`
                Object {
                  "bodyVersion": 0,
                  "date": 0,
                  "guid": 0,
                  "preview": "[Decrypted message] Basic Html Content",
                  "smartAttach": false,
                  "structure": Object {
                    "address": "1",
                    "dispositionType": "INLINE",
                    "encoding": "",
                    "mime": "text/html",
                    "size": 118,
                  },
                }
            `);
        });
        test("multi_alternative", async () => {
            const eml = readEml("multi_alternative");
            const structure = await EmlParser.parseBodyStructure(body, eml);
            expect(structure).toMatchInlineSnapshot(`
                Object {
                  "bodyVersion": 0,
                  "date": 0,
                  "guid": 0,
                  "preview": "[Decrypted message] Multi Alternative Content--X-BM-SIGNATURE--",
                  "smartAttach": false,
                  "structure": Object {
                    "address": "TEXT",
                    "children": Array [
                      Object {
                        "address": "1",
                        "dispositionType": "INLINE",
                        "encoding": "",
                        "mime": "text/plain",
                        "size": 45,
                      },
                      Object {
                        "address": "2",
                        "dispositionType": "INLINE",
                        "encoding": "",
                        "mime": "text/html",
                        "size": 126,
                      },
                    ],
                    "mime": "multipart/alternative",
                    "size": 0,
                  },
                }
            `);
        });
        test("multi_related", async () => {
            const eml = readEml("multi_related");
            const structure = await EmlParser.parseBodyStructure(body, eml);
            expect(structure).toMatchInlineSnapshot(`
                Object {
                  "bodyVersion": 0,
                  "date": 0,
                  "guid": 0,
                  "preview": "[Decrypted message] Multi Related Content",
                  "smartAttach": false,
                  "structure": Object {
                    "address": "TEXT",
                    "children": Array [
                      Object {
                        "address": "1",
                        "dispositionType": "INLINE",
                        "encoding": "",
                        "mime": "text/html",
                        "size": 223,
                      },
                      Object {
                        "address": "2",
                        "contentId": "<0d196b557d8acd1f1f81a9f96a331f4c@bluemind.net>",
                        "dispositionType": "INLINE",
                        "encoding": "",
                        "fileName": "smile.png",
                        "mime": "image/png",
                        "size": 1738,
                      },
                    ],
                    "mime": "multipart/related",
                    "size": 0,
                  },
                }
            `);
        });
        test("multi_mixed", async () => {
            const eml = readEml("multi_mixed");
            const structure = await EmlParser.parseBodyStructure(body, eml);
            expect(structure).toMatchInlineSnapshot(`
                Object {
                  "bodyVersion": 0,
                  "date": 0,
                  "guid": 0,
                  "preview": "[Decrypted message] Multi Mixed Content",
                  "smartAttach": true,
                  "structure": Object {
                    "address": "TEXT",
                    "children": Array [
                      Object {
                        "address": "1",
                        "dispositionType": "INLINE",
                        "encoding": "",
                        "mime": "text/plain",
                        "size": 22,
                      },
                      Object {
                        "address": "2",
                        "dispositionType": "ATTACHMENT",
                        "encoding": "",
                        "fileName": "smile.png",
                        "mime": "image/png",
                        "size": 1738,
                      },
                    ],
                    "mime": "multipart/mixed",
                    "size": 0,
                  },
                }
            `);
        });
        test("multi_full", async () => {
            const eml = readEml("multi_full");
            const structure = await EmlParser.parseBodyStructure(body, eml);
            expect(structure).toMatchInlineSnapshot(`
                Object {
                  "bodyVersion": 0,
                  "date": 0,
                  "guid": 0,
                  "preview": "[Decrypted message] Multi Full Content",
                  "smartAttach": true,
                  "structure": Object {
                    "address": "TEXT",
                    "children": Array [
                      Object {
                        "address": "1",
                        "children": Array [
                          Object {
                            "address": "1.1",
                            "dispositionType": "INLINE",
                            "encoding": "",
                            "mime": "text/plain",
                            "size": 24,
                          },
                          Object {
                            "address": "1.2",
                            "children": Array [
                              Object {
                                "address": "1.2.1",
                                "dispositionType": "INLINE",
                                "encoding": "",
                                "mime": "text/html",
                                "size": 322,
                              },
                              Object {
                                "address": "1.2.2",
                                "contentId": "<a0fdd7347c653e453159dd85588d6ad6@bluemind.net>",
                                "dispositionType": "INLINE",
                                "encoding": "",
                                "fileName": "image-a0fdd.png",
                                "mime": "image/png",
                                "size": 1738,
                              },
                            ],
                            "mime": "multipart/related",
                            "size": 0,
                          },
                        ],
                        "mime": "multipart/alternative",
                        "size": 0,
                      },
                      Object {
                        "address": "2",
                        "dispositionType": "ATTACHMENT",
                        "encoding": "",
                        "fileName": "smile.png",
                        "mime": "image/png",
                        "size": 1738,
                      },
                    ],
                    "mime": "multipart/mixed",
                    "size": 0,
                  },
                }
            `);
        });
    });
});

function readEml(file) {
    return fs.readFileSync(path.join(__dirname, `./datas/simple/${file}.eml`), "utf8", (err, data) => data);
}
