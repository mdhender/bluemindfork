export default {
    beforeCreate: function() {
        if (this.$parent && this.$parent.$i18n) {
            const locales = [this.$i18n.locale, this.$i18n.fallbackLocale];
            if (this.$options.componentI18N && this.$options.componentI18N.messages) {
                /* if component has been instantiate  with "componentI18N: { ... }" 
                then we merge component i18n messages with parent messages */
                locales.forEach(key => {
                    this.$i18n.mergeLocaleMessage(key, this.$options.componentI18N.messages[key] || {});
                });
            }
        }
    }
};
