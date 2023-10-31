import DependencyLocator from "./DependencyLocator";

const inject = ($vm, method) => {
    const hooks = $vm.$options[method];
    if (hooks && hooks.length > 0) {
        hooks[hooks.length - 1] = DependencyLocator.inject(hooks[hooks.length - 1], $vm);
    }
};

export default {
    install(Vue) {
        Vue.mixin({
            beforeCreate() {
                inject(this, "created");
            }
        });
    }
};
