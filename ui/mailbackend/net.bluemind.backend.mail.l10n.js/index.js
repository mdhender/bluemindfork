import { TranslationHelper } from "@bluemind/i18n";

const properties = TranslationHelper.loadTranslations(require.context('./l10n', false, /\.json$/));

export default properties;

export function getLocalizedProperty(userSession, propertyKey, namedParameters) {
    // FIXME should use a common tool to translate messages (see '@bluemind/webapp.mail.l10n')
    let property = properties[userSession.lang][propertyKey];
    if (namedParameters) {
        namedParameters = new Map(Object.entries(namedParameters));
        namedParameters.forEach(
            (value, key) => property = property.replace(new RegExp("\\{" + key + "\\}"), value));
    }
    return property;
}