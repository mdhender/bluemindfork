export default {
    install(Vue, VueBus) {
        Vue.prototype.$bus = new VueBus.Client();

        Vue.mixin({
            beforeCreate() {
                const listeners = this.$options.bus;
                if (listeners) {
                    Object.keys(listeners).forEach(event => {
                        const handler = (event, payload) => listeners[event].call(this, payload);
                        this.$bus.$on(event, handler);
                        listeners[event]._handler = handler;
                    });
                }
            },
            beforeDestroy() {
                const listeners = this.$options.bus;
                if (listeners) {
                    Object.keys(listeners).forEach(event => {
                        this.$bus.$off(event, listeners[event]._handler);
                    });
                }
            }
        });
    }
};
