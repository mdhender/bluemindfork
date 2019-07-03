import TranslationHelper from "./TranslationHelper";
import VueI18n from "vue-i18n";

export default {
    beforeCreate: function() {
        if (this.$parent && this.$parent.$i18n) {
            if (this.$options.i18n && this.$options.i18n.messages) {
                /* if component has been instantiate  with "i18n: blabla" 
                        then we merge component i18n messages with parent messages */
                this._i18n = new VueI18n({
                    locale: this._i18n.locale,
                    fallbackLocale: this._i18n.fallbackLocale,
                    messages: TranslationHelper.mergeTranslations(
                        this.$parent.$i18n.messages, 
                        this.$options.i18n.messages
                    )
                });
            } else {
            // if no i18n defined, component inherits from its parent if possible
                this._i18n = this.$parent.$i18n;
            }
        }
    }
};