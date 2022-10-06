export default {
    beforeCreate: function () {
        if (this.$parent?.$i18n && this.$options.componentI18N?.messages) {
            const locales = [this.$i18n.locale, this.$i18n.fallbackLocale];
            /* if component has been instantiate  with "componentI18N: { ... }" 
                then we merge component i18n messages with parent messages */
            locales.forEach(key => {
                this.$i18n.mergeLocaleMessage(key, this.$options.componentI18N.messages[key] || {});
            });
        }
    }
};
