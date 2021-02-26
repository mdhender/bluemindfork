import { TranslationHelper } from "@bluemind/i18n";

export default TranslationHelper.loadTranslations(require.context("./files", false, /\.json$/));
