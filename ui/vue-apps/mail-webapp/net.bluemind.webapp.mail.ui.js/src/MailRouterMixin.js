export default {
    methods: {
        computeMessageRoute(currentFolderKey, messageKey = "", messageFilter) {
            const path = this.$route.path;
            const filter = messageFilter ? "?filter=" + messageFilter : "";
            if (this.$route.params.mail) {
                return path.replace(new RegExp("/" + this.$route.params.mail + "/?.*"), "/" + messageKey) + filter;
            } else if (path === "/mail/" || path === "/mail/new") {
                return "/mail/" + currentFolderKey + "/" + messageKey + filter;
            }
            return path + messageKey + filter;
        }
    }
};
