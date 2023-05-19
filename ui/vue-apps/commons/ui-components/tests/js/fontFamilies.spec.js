import FONT_FAMILIES, { fontFamilyByID } from "../../src/js/fontFamilies";

describe("fontFamilies", () => {
    test("throw an error when no id is given", () => {
        expect(() => fontFamilyByID("")).toThrowError();
    });

    describe("Given a font id", () => {
        test("should throw an error if no match found", () => {
            expect(() => fontFamilyByID("unknownFontId")).toThrowError();
        });

        test("should return css value of font family with its fallbacks font if a match found ", () => {
            FONT_FAMILIES.forEach(font => {
                expect(fontFamilyByID(font.id)).toBe(font.value);
            });
        });

        test("can extent list of matching font families by providing an extra object in parameters", () => {
            const extraFonts = [
                {
                    id: "extra",
                    text: "Extra",
                    value: "Extra, extra, Extra Value"
                }
            ];

            expect(fontFamilyByID("extra", extraFonts)).toBe("Extra, extra, Extra Value");
        });
    });
});
