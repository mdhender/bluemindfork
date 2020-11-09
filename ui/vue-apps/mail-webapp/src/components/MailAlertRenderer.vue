<script>
import MailFolderIcon from "./MailFolderIcon";

export default {
    name: "MailAlertRenderer",
    components: {
        MailFolderIcon
    },
    props: {
        alert: {
            type: Object,
            default: () => ({})
        }
    },
    /** Build the component via javascript. It avoids unwanted spaces when rendered. */
    render(createElement) {
        const alert = this.alert;
        const children = [];
        alert.props = alert.props ? alert.props : {};

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
        const mailFolderIcon = createElement("mail-folder-icon", {
            attrs: { place: "icon", folder: alert.props.folder }
        });
        children.push(
            createElement(
                "router-link",
                {
                    attrs: { place: "folderNameWithLink" },
                    class: "ml-1",
                    props: { to: alert.props.folderNameLink }
                },
                [mailFolderIcon]
            )
        );

        children.push(
            createElement("mail-folder-icon", {
                attrs: {
                    place: "oldFolderName",
                    folder: alert.props.oldFolder
                },
                class: "ml-1"
            })
        );

        children.push(createElement("br", { attrs: { place: "br" } }));

        children.push(createElement("a", { attrs: { place: "reloadLink", href: "." } }, [this.$t("common.reload")]));

        return createElement(
            "i18n",
            { props: { path: alert.key, places: alert.props }, class: "mail-alert-renderer" },
            children
        );
    }
};
</script>

<style lang="scss" scoped>
@import "@bluemind/styleguide/css/_variables.scss";

.mail-alert-renderer {
    a,
    a:visited {
        font-style: italic !important;
        color: theme-color("dark") !important;

        .mail-folder-icon {
            text-decoration: underline;
        }
    }

    .mail-folder-icon {
        font-style: normal !important;
        font-weight: bold;
    }
}
</style>
