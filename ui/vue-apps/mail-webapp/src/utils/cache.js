export class Cache extends Map {
    constructor(iterableOrLoader, loader) {
        let iterable = typeof iterableOrLoader !== "function" ? iterableOrLoader : undefined;
        super(iterable);
        this.loader = typeof iterableOrLoader === "function" ? iterableOrLoader : loader;
    }
    get(key, loader) {
        if (!this.has(key)) {
            this.set(key, loader ? loader(key) : this.loader(key));
        }
        return super.get(key);
    }
}
