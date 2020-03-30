export default {
    methods: {
        /** Navigate to the parent path: from a/b/c to a/b */
        navigateToParent() {
            const currentRoute = this.$router.history.current;
            let parentPath = currentRoute.path.substring(0, currentRoute.path.lastIndexOf("/") + 1);
            if (currentRoute.query.filter) {
                parentPath += "?filter=" + currentRoute.query.filter;
            }
            this.$router.push(parentPath);
        }
    }
};
