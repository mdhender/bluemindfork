export default {
    beforeCreate: function () {
        /* if component has been instantiate  with "componentI18N: { ... }" 
                then we merge component i18n messages with parent messages */
        if (this.$parent?.$i18n && this.$options.componentI18N?.messages) {
            const locales = [this.$i18n.locale, this.$i18n.fallbackLocale];
            const messages = Array.isArray(this.$options.componentI18N.messages)
                ? this.$options.componentI18N.messages
                : [this.$options.componentI18N.messages];
            locales.forEach(key => {
                messages.forEach(msgs => {
                    this.$i18n.mergeLocaleMessage(key, msgs[key] || {});
                });
            });
        }
    }
};
