export default function OldAppWrapper(name, url) {
    return {
        extends: WrapperComponent,
        name,
        data: () => ({ url })
    };
}
const WrapperComponent = {
    name: "OldAppWrapper",
    render(h) {
        return h("iframe", { attrs: { src: this.url }, staticClass: "flex-fill border-0 bg-surface" });
    }
};
