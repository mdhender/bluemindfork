<script>
export default {
    name: "MailAlertRenderer",
    functional: true,
    props: {
        alert: {
            type: Object,
            default: () => {}
        }
    },
    /** Build the component via javascript. It avoids unwanted spaces when rendered. */
    render(createElement, { props }) {
        const alert = props.alert;
        const children = [];
        children.push(
            createElement(
                "router-link",
                {
                    attrs: { place: "subjectWithLink" },
                    props: { to: alert.props.subjectLink }
                },
                [alert.props.subject]
            )
        );
        children.push(createElement("i", { attrs: { place: "subject" } }, [alert.props.subject]));
        children.push(createElement("br", { attrs: { place: "br" } }));
        return createElement("i18n", { props: { path: alert.key, places: alert.props } }, children);
    }
};
</script>

<style lang="scss" scoped>
@import "@bluemind/styleguide/css/_variables.scss";

a, a:visited {
    font-style: italic !important;
    color: theme-color("dark") !important;
}

</style>
