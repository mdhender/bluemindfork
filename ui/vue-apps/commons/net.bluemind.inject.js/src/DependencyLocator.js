import { annotate } from "./annotate";
import isFunction from "lodash.isfunction";
import isString from "lodash.isstring";
import isNil from "lodash.isnil";
import isUndefined from "lodash.isundefined";
import global from "@bluemind/global";

const $injector = global.$injector || (global.$injector = { dependencies: new Map() });
const dependencies = $injector.dependencies;

const DependencyLocaltor = {
    register(provider) {
        if (Array.isArray(provider)) {
            provider.forEach(p => this.register(p));
        } else {
            const { provide, factory } = annotateProvider(provider);
            if (dependencies.get(provide)) {
                console.warn("Watch out ! you're registering a listener already set : " + provide);
            }
            dependencies.set(provide, factory);
        }
        return this;
    },

    getProvider(provider) {
        if (isFunction(provider) && isString(provider.name)) {
            provider = provider.name;
        }
        if (dependencies.has(provider)) {
            return { get: apply(dependencies.get(provider)) };
        }
        return { get: () => null };
    },

    inject(fn, thisArg) {
        annotate(fn);
        return apply(fn, thisArg);
    }
};

export default DependencyLocaltor;

export function inject(provider, ...params) {
    return DependencyLocaltor.getProvider(provider).get(...params);
}

function annotateProvider(provider) {
    if (isUndefined(provider.provide)) {
        provider = {
            provide: provider
        };
    }
    let { provide, use, factory } = provider;
    if (isNil(use)) {
        use = provide;
    }
    if (!isString(provide) && isString(provide.name)) {
        provide = provide.name;
    }
    if (isNil(factory)) {
        if (isFunction(use) && isString(use.name)) {
            factory = (...args) => new use(...args);
            factory.$inject = annotate(use.prototype.constructor);
        } else {
            factory = () => use;
        }
    }
    annotate(factory);
    return { provide, factory };
}

function apply(providerFn, thisArg) {
    const deps = providerFn.$inject.map(arg =>
        dependencies.has(arg) ? apply(dependencies.get(arg), thisArg) : undefined
    );
    return (...args) => {
        const finals = providerFn.$inject.map((key, index) => {
            return !isNil(deps[index]) ? deps[index]() : args.shift();
        });
        return thisArg ? providerFn.call(thisArg, ...finals) : providerFn(...finals);
    };
}
