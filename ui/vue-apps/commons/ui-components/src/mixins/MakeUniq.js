export default {
    methods: {
        makeUniq(prefix = "") {
            return prefix + this._uid;
        }
    }
};
