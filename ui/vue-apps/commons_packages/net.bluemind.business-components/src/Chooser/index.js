import { TranslationRegistry } from "@bluemind/i18n";
import store from "@bluemind/store";
import { chooserStore } from "./store/store";
import ChooserL10N from "../l10n";

TranslationRegistry.register(ChooserL10N);
if (!store.hasModule("chooser")) {
    store.registerModule("chooser", chooserStore);
}

export { default as Chooser } from "./Chooser";
