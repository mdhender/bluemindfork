import Vuex from "vuex";

export default class BmVuexStore extends Vuex.Store {
    // inspired from Vuex subscribe & subscribeAction: https://github.com/vuejs/vuex/blob/main/src/store.js#L199
    subscribeModule(sub, options) {
        if (!this._moduleSubscribers) {
            // cant do it in constructor because Vuex constructor calls subscribeModule
            this._moduleSubscribers = [];
        }
        sub = normalizeModuleSubscriber(sub);
        if (this._moduleSubscribers.indexOf(sub) < 0) {
            options && options.prepend ? this._moduleSubscribers.unshift(sub) : this._moduleSubscribers.push(sub);
        }
        return () => {
            const i = this._moduleSubscribers.indexOf(sub);
            if (i > -1) {
                this._moduleSubscribers.splice(i, 1);
            }
        };
    }

    registerModule(path) {
        try {
            this._moduleSubscribers.forEach(sub => sub.before(...arguments));
        } catch (e) {
            // eslint-disable-next-line no-console
            console.error("before registerModule subscriber failed for module ", path, e);
        }
        super.registerModule(...arguments);
        try {
            this._moduleSubscribers.forEach(sub => sub.after(...arguments));
        } catch (e) {
            // eslint-disable-next-line no-console
            console.error("after registerModule subscriber failed for module ", path, e);
        }
    }
}

function normalizeModuleSubscriber(subscriber) {
    if (!subscriber.before) {
        subscriber.before = () => {};
    }
    if (!subscriber.after) {
        subscriber.after = () => {};
    }
    return subscriber;
}
