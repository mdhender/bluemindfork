<script>
export default {
    name: "BmListGroupSeparator",
    props: {
        tag: {
            type: String,
            default: "div"
        }
    },
    render(h) {
        return h(this.$props.tag, { class: "list-group-separator" }, this.$slots.default);
    }
};
</script>

<style lang="scss">
@import "../../css/_variables.scss";

.list-group-separator {
    position: relative;
    display: block;
    padding: $sp-2;
}
</style>
