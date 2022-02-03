import Vue from "vue";

export default {
    methods: {
        print(body, style) {
            const content = new PrintContent({ parent: this.$parent });
            content.$slots.body = body;
            content.$slots.style = style;
            const print = document.createElement("iframe");
            print.style.width = "0";
            print.style.height = "0";
            print.addEventListener("load", () => {
                content.$mount(print.contentDocument.body);
                print.contentWindow.addEventListener("afterprint", () => print.remove());
                setTimeout(() => print.contentWindow.print(), 250);
            });
            document.body.appendChild(print);
        }
    }
};
const PrintContent = Vue.extend({
    render(h) {
        return h("body", [h("style", this.$slots.style), this.$slots.body]);
    }
});
