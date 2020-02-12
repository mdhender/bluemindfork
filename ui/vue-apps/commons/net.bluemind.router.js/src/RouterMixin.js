export default {
    methods: {
        /** Navigate to the parent path: from a/b/c to a/b */
        navigateToParent() {
            const path = this.$router.history.current.path;
            const parentPath = path.substring(0, path.lastIndexOf("/") + 1);
            this.$router.push(parentPath);
        }
    }
};
