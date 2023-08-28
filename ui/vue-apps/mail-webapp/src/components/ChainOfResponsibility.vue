<script>
export default {
    name: "ChainOfResponsibility",
    props: { isResponsible: { type: Boolean, required: true } },
    render() {
        return this.isResponsible ? this.$scopedSlots.default() : this.$parent.$scopedSlots.default();
    }
};
</script>
