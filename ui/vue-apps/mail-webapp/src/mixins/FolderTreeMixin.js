import { AppDataKeys } from "@bluemind/webappdata";
import { MY_MAILBOX_KEY } from "~/getters";

export default {
    data() {
        return { expanded: false };
    },
    mounted() {
        const appData = this.$store.state["root-app"].appData[AppDataKeys.MAIL_FOLDER_TREES_COLLAPSED];
        const value = appData ? appData.value : [];
        const index = value.findIndex(({ treeKey }) => treeKey === this.treeKey);
        if (index === -1) {
            const areExpandedByDefault = [this.$store.getters["mail/" + MY_MAILBOX_KEY]];
            this.expanded = areExpandedByDefault.includes(this.treeKey);
        } else {
            this.expanded = !value[index].isCollapsed;
        }
    },
    methods: {
        toggleTree() {
            this.expanded = !this.expanded;

            const appDataKey = AppDataKeys.MAIL_FOLDER_TREES_COLLAPSED;
            const appData = this.$store.state["root-app"].appData[appDataKey];
            const value = appData ? appData.value : [];
            const index = value.findIndex(({ treeKey }) => treeKey === this.treeKey);

            const isCollapsed = !this.expanded;
            if (index === -1) {
                value.push({ treeKey: this.treeKey, isCollapsed });
            } else {
                value[index].isCollapsed = isCollapsed;
            }

            this.$store.dispatch("root-app/SET_APP_DATA", { key: appDataKey, value });
        }
    }
};
