<script>
export default {
    name: "BmExtensionChainOfResponsibility",
    functional: true,
    props: {
        extensions: {
            type: Array,
            default: () => []
        }
    },
    render: function (h, { props, data, scopedSlots }) {
        const defaultSlot = scopedSlots.default ? scopedSlots.default() : null;
        const render = [...props.extensions].reverse().reduce(
            (renderPrevious, current) => {
                const renderFn = () => h(current.name, { props: { ...data.attrs, next: renderPrevious } });
                return renderFn;
            },
            () => defaultSlot
        );
        return render();
    }
};
</script>
