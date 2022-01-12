<script>
export default {
    name: "InlineStyle",
    functional: true,
    render(h, { slots }) {
        return h("style", slots().default);
    }
};
</script>
