export default {
    methods: {
        $waitFor(subject, opt_assert, options = { immediate: true, deep: true }) {
            let resolver;
            const assert = opt_assert || Boolean;
            if (!assert(subject)) {
                const promise = new Promise(resolve => (resolver = resolve));
                const unwatch = this.$watch(subject, value => assert(value) && resolver(), options);
                promise.then(unwatch);
                return promise;
            }
        }
    }
};
