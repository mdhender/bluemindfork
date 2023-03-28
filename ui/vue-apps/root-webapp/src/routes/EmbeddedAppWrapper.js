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
        return h("iframe", { attrs: { src: this.url }, staticClass: "flex-fill border-0 bg-surface" });
    }
};
