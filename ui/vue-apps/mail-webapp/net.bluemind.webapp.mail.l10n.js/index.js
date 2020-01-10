import { TranslationHelper } from "@bluemind/i18n";

export default TranslationHelper.loadTranslations(require.context('./l10n', false, /\.json$/));