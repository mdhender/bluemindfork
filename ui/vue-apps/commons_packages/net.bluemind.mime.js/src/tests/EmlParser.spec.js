import fs from "fs";
import path from "path";
import EmlParser from "../EmlParser";

describe("EmlPartParser", () => {
    describe("body", () => {
        test("basic", async () => {
            const eml = readEml("basic_text");
            const parser = await new EmlParser({}).parse(eml);
            expect(parser.body.date).toEqual(new Date(" Fri, 2 Sep 2022 15:04:59 +0100").getTime());
            expect(parser.body.smartAttach).toBeFalsy();
            expect(parser.body.preview).toEqual("Basic Text content");
            expect(parser.body.messageId).toEqual("<92F23A02-9EDE-474B-952E-840A95A25802@test.bluemind.net>");
            expect(parser.body.recipients).toContainEqual({
                address: "mehdi@bm.lan",
                dn: "Mehdi Rande",
                kind: "Originator"
            });
            expect(parser.body.recipients).toContainEqual({
                address: "mehdi@blue-mind.loc",
                dn: "Mehdi Rande",
                kind: "Primary"
            });
            expect(parser.body.headers.length).toEqual(0);
            expect(parser.body.structure).toBe(parser.structure);
            expect(parser.body.structure.address).toBe("1");
        });
    });
});

function readEml(file) {
    return fs.readFileSync(path.join(__dirname, `./datas/eml/${file}.eml`), "utf8", (err, data) => data);
}
