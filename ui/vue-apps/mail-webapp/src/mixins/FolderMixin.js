export default {
    methods: {
        expand(key) {
            this.toggle(key, true);
        },
        toggle(key, forceExpand = false) {
            const wasExpanded = this.$store.state.mail.folderList.expandedFolders.indexOf(key) > -1;
            const expanded = forceExpand || !wasExpanded;
            this.$store.commit("mail/SET_FOLDER_EXPANDED", { key, expanded });
        }
    }
};
