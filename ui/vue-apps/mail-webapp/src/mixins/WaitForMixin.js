export default {
    methods: {
        $waitFor(subject, opt_assert) {
            let resolver;
            const assert = opt_assert || Boolean;
            const promise = new Promise(resolve => (resolver = resolve));
            const unwatch = this.$watch(subject, value => assert(value) && resolver(), { immediate: true, deep: true });
            promise.then(unwatch);
            return promise;
        }
    }
};
