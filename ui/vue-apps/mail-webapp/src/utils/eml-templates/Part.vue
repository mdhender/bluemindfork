<script>
export default {
    name: "Part",
    props: {
        charset: { type: String, default: undefined },
        dispositionType: { type: String, default: undefined },
        encoding: { type: String, default: undefined },
        fileName: { type: String, default: undefined },
        mime: { type: String, required: true },
        subject: { type: String, default: "" }
    },
    methods: {
        structure() {
            const struct = { ...this._props };
            Object.keys(struct).forEach(k => !struct[k] && delete struct[k]);
            if (this.$children?.length) {
                struct.children = [];
                this.$children
                    .filter(child => child._isVue && child.$options.name === "Part")
                    .forEach(child => struct.children.push(child.structure()));
            } else {
                struct.content = this.mime.includes("/html") ? this.$el.innerHTML : this.$el.textContent;
            }
            return struct;
        }
    },
    render(createElement) {
        return createElement("div", {}, this.$scopedSlots.default());
    }
};
</script>
