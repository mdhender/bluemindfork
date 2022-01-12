<script>
import Vue from "vue";

export default {
    name: "IFrame",
    props: {
        fitContent: {
            type: Boolean,
            default: true
        }
    },
    data() {
        return {
            body: new Vue({
                data: { content: Object.freeze(this.$slots.default) },
                render(h) {
                    return h("body", [this.content]);
                }
            }),
            head: new Vue({
                data: { content: Object.freeze(this.$slots.head), style: Object.freeze(this.$slots.style) },
                render(h) {
                    return h("head", [this.content, h("style", this.style)]);
                }
            }),
            resize: null
        };
    },
    destroyed() {
        this.resize?.disconnect();
        this.head?.$destroy();
        this.body?.$destroy();
    },
    updated() {
        this.body.content = Object.freeze(this.$slots.default);
        this.head.content = Object.freeze(this.$slots.head);
        this.head.style = Object.freeze(this.$slots.style);
    },
    methods: {
        mount() {
            const doc = this.$el.contentDocument;

            if (this.fitContent) {
                //https://bugzilla.mozilla.org/show_bug.cgi?id=1689099
                this.resize = new this.$el.contentWindow.ResizeObserver(([content]) => {
                    this.$el.style.height = content.contentRect.height + "px";
                    this.$emit("resized", content.contentRect);
                });
                this.resize.observe(doc.documentElement);
            }

            this.body.content = Object.freeze(this.$slots.default);
            this.head.content = Object.freeze(this.$slots.head);
            this.head.style = Object.freeze(this.$slots.style);
            this.head.$mount(doc.head);
            this.body.$mount(doc.body);

            // Some DevMode feature doesn't work without this...
            doc.addEventListener("click", () => this.$el.click());
        }
    },
    render(h) {
        // Src doc is needed to have the rigth dtd.
        return h("iframe", { class: "i-frame", on: { load: this.mount }, domProps: { srcdoc: "<html></html>" } });
    }
};
</script>
