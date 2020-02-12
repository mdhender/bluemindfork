import TranslationHelper from "../src/TranslationHelper";

describe("TranslationHelper", () => {
    test("mergeTranslations can merge multiple langs", () => {
        const lang1 = {
            en: {
                "my.key": "my.value"
            }
        };
        const lang2 = {
            fr: {
                "my.key": "ma.valeur"
            }
        };
        const mergedTranslations = TranslationHelper.mergeTranslations(lang1, lang2);
        expect(mergedTranslations).toHaveProperty("en");
        expect(mergedTranslations).toHaveProperty("fr");
    });

    test("mergeTranslations can merge keys for a same language", () => {
        const trad1 = {
            en: {
                "my.first.key": "my.value"
            }
        };
        const trad2 = {
            en: {
                "my.second.key": "my.value"
            }
        };
        const mergedTranslations = TranslationHelper.mergeTranslations(trad1, trad2);
        expect(mergedTranslations).toHaveProperty("en");
        expect(mergedTranslations["en"]).toEqual(
            expect.objectContaining({
                "my.first.key": expect.any(String),
                "my.second.key": expect.any(String)
            })
        );
    });

    test("mergeTranslations merge 2nd parameter value when both parameters have a same key for a same lang", () => {
        const trad1 = {
            en: {
                "my.key": "my.first.value"
            }
        };
        const trad2 = {
            en: {
                "my.key": "my.second.value"
            }
        };
        const mergedTranslations = TranslationHelper.mergeTranslations(trad1, trad2);
        expect(mergedTranslations["en"]["my.key"]).toEqual("my.second.value");
    });

    // FIXME: to make this test works, we need to mock require.context
    // test('loadTranslations merge 2 translations files in one object', () => {
    //     let result = TranslationHelper.loadTranslations(require.context('./l10n', false, /\.json$/));
    // });
});
