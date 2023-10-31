import i18n from "./i18n";

export default {
    register(l10n) {
        const locales = [i18n.locale, i18n.fallbackLocale];
        locales.forEach(key => {
            i18n.mergeLocaleMessage(key, l10n[key] || {});
        });
    }
};
