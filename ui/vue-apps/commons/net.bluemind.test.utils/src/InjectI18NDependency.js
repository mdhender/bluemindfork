import Vue from "vue";
import VueI18n from "vue-i18n";
import { generateDateTimeFormats } from "@bluemind/i18n";
import CommonL10N from "@bluemind/l10n";
import injector from "@bluemind/inject";

jest.mock("@bluemind/styleguide", () => ({
    StyleguideL10N: {
        en: {},
        fr: {}
    }
}));

export default {
    registerCommonL10N: () => {
        Vue.use(VueI18n);

        const dateTimeFormat = generateDateTimeFormats("")["fr"];

        injector.register({
            provide: "i18n",
            use: new VueI18n({
                locale: "fr",
                fallbackLocale: "fr",
                dateTimeFormats: { fr: dateTimeFormat },
                messages: CommonL10N
            })
        });
    }
};
