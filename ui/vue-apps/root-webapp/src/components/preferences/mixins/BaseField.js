export default {
    props: {
        id: {
            type: String,
            required: true
        },
        disabled: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    methods: {
        NEED_RELOAD() {
            this.$store.commit("preferences/fields/NEED_RELOAD", { id: this.id });
        },
        PUSH_STATE(state) {
            this.$store.commit("preferences/fields/PUSH_STATE", { id: this.id, ...state });
        }
    },
    mounted() {
        if (!this.$store.hasModule(["preferences", "fields", this.id])) {
            this.$store.registerModule(["preferences", "fields", this.id], { state: { current: null, saved: null } });
        }
    },
    destroyed() {
        if (this.$store.hasModule(["preferences", "fields", this.id])) {
            this.$store.unregisterModule(["preferences", "fields", this.id]);
        }
    }
};
