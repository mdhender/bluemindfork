import UUIDGenerator from "@bluemind/uuid";
import Vue from "vue";

export default function EmbeddedAppWrapper(name, url) {
    return {
        extends: WrapperComponent,
        name,
        data: () => ({ url })
    };
}
const WrapperComponent = {
    name: "EmbeddedAppWrapper",
    render(h) {
        return h("iframe", {
            attrs: { src: this.src },
            on: { load: this.forwardNotice },
            staticClass: "flex-fill border-0 bg-white"
        });
    },
    computed: {
        src() {
            const url = new URL(this.url, document.baseURI);
            for (let param in this.$route.query) {
                url.searchParams.set(param, this.$route.query[param]);
            }
            url.hash = this.$route.hash;
            return url.toString();
        }
    },
    methods: {
        forwardNotice() {
            this.$el.contentDocument.addEventListener("ui-notification", e => {
                const type = e.detail?.type !== "error" ? "SUCCESS" : "ERROR";
                this.$store.dispatch("alert/" + type, {
                    alert: { uid: UUIDGenerator.generate(), name: "embedded-notification", payload: e.detail?.message },
                    options: { renderer: "EmbeddedNotificationAlert" }
                });
            });
        }
    }
};
Vue.component("EmbeddedNotificationAlert", {
    functional: true,
    props: {
        alert: {
            type: Object,
            default: () => ({})
        }
    },
    render: (h, { props }) => h("span", props.alert.payload)
});
