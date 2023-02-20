<script>
export default {
    name: "ChainOfResponsibility",
    props: {
        isResponsible: {
            type: Boolean,
            required: true
        }
    },
    render() {
        if (this.isResponsible) {
            return this.$scopedSlots.default();
        }
        return this.$parent.$scopedSlots.default();
    }
};
</script>
