import { TranslationHelper } from '@bluemind/i18n';
import { StyleguideL10N } from '@bluemind/styleguide';

const CommonL10N = TranslationHelper.loadTranslations(require.context('./l10n', false, /\.json$/));
export default TranslationHelper.mergeTranslations(CommonL10N, StyleguideL10N);