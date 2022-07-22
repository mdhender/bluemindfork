// import { AppDataKeys } from "@bluemind/webappdata";

export default {
    methods: {
        expand(key) {
            this.toggle(key, true);
        },
        toggle(key, forceExpand = false) {
            const folder = this.$store.state.mail.folders[key];
            const isExpanded = forceExpand || !folder.expanded;
            this.$store.commit("mail/SET_FOLDER_EXPANDED", { ...folder, expanded: isExpanded });

            this.$FolderMixin_updateAppData(key, isExpanded);
        }
        // $FolderMixin_updateAppData(key, isExpanded) {
        // const appDataKey = AppDataKeys.MAIL_FOLDERS_EXPANDED;
        // const appData = this.$store.state["root-app"].appData[appDataKey];
        // const value = appData ? appData.value : [];
        // const index = value.indexOf(key);
        // if (!isExpanded && index > -1) {
        //     value.splice(index, 1);
        // }
        // if (isExpanded && index === -1) {
        //     value.push(key);
        // }
        // this.$store.dispatch("root-app/SET_APP_DATA", { key: appDataKey, value });
        // }
    }
};
