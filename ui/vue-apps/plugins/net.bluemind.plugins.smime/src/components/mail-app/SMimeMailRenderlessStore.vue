<script>
import SMimeMailStore from "../../store/mail/";

export default {
    name: "SMimeMailRenderlessStore",
    beforeCreate() {
        if (this.$store.hasModule("mail") && !this.$store.hasModule(["mail", "smime"])) {
            this.$store.registerModule(["mail", "smime"], SMimeMailStore);
        }
    },
    destroyed() {
        if (this.$store.hasModule(["mail", "smime"])) {
            this.$store.unregisterModule(["mail", "smime"]);
        }
    },
    render() {
        return "";
    }
};
</script>
