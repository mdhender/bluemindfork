<script>
import FileHostingStore from "../store/";

export default {
    name: "RenderlessStore",
    beforeCreate() {
        if (this.$store.hasModule("mail") && !this.$store.hasModule(["mail", "filehosting"])) {
            this.$store.registerModule(["mail", "filehosting"], FileHostingStore);
        }
    },
    destroyed() {
        if (this.$store.hasModule(["mail", "filehosting"])) {
            this.$store.unregisterModule(["mail", "filehosting"]);
        }
    },
    render() {
        return "";
    }
};
</script>
