import Vue from "vue";
import VueI18n from "vue-i18n";
import global from "@bluemind/global";
import CommonL10N from "@bluemind/l10n";
import generateDateTimeFormats from "./DateTimeFormats";
import TranslationHelper from "./TranslationHelper";

function initI18n() {
    Vue.use(VueI18n);
    return new VueI18n({
        dateTimeFormats: generateDateTimeFormats(),
        locale: window.bmcSessionInfos.lang,
        messages: TranslationHelper.loadTranslations(CommonL10N),
        fallbackLocale: navigator.language ? navigator.language.split("-")[0] : "en"
    });
}

export default global.$i18n || (global.$i18n = initI18n());
