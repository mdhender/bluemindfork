<script>
export default {
    name: "BmExtensionChainOfResponsibility",
    props: {
        extensions: {
            type: Array,
            default: () => []
        }
    },
    render: function (h) {
        const defaultSlot = this.$scopedSlots.default ? this.$scopedSlots.default() : null;
        const render = [...this.extensions].reverse().reduce(
            (renderPrevious, current) => {
                const renderFn = () => h(current.name, { props: { ...this.$attrs, next: renderPrevious } });
                return renderFn;
            },
            () => defaultSlot
        );
        return render();
    }
};
</script>
