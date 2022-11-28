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
                data: { content: null },
                render(h) {
                    return h("body", [this.content]);
                }
            }),
            head: new Vue({
                data: { content: null, style: null },
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
                this.resize = new this.$el.contentWindow.ResizeObserver(([{ contentRect }]) => {
                    this.$el.style.height = contentRect.height + "px";
                    this.$el.style.height = fixScrollbarOffset(this.$el, doc.documentElement) + "px";
                    this.$emit("resized", contentRect);
                });
                this.resize.observe(doc.documentElement);
                doc.documentElement.style.overflowY = "hidden";
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
        // Src or srcdoc is needed to have the rigth dtd.
        // Use of src instead of srcdoc to force the SW to intercept the request,
        // a workaround for this bug is Chrome: https://github.com/w3c/ServiceWorker/issues/765
        return h("iframe", { class: "i-frame", on: { load: this.mount }, domProps: { src: "/webapp/blank" } });
    }
};

// Fix a bug with Firefox and big images.
function fixScrollbarOffset(iframe, html) {
    if (iframe.offsetHeight === html.offsetHeight && html.offsetHeight > html.clientHeight) {
        return iframe.offsetHeight + (html.offsetHeight - html.clientHeight);
    }
    return iframe.offsetHeight;
}
</script>
