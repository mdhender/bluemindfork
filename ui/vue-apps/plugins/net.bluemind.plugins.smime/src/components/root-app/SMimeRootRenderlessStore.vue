<script>
import BmRoles from "@bluemind/roles";
import session from "@bluemind/session";
import SMimeRootStore from "../../store/root-app/";
import { CHECK_IF_ASSOCIATED, SET_SW_AVAILABLE } from "../../store/root-app/types";

export default {
    name: "SMimeRootRenderlessStore",
    beforeCreate() {
        if (!this.$store.hasModule("smime")) {
            this.$store.registerModule("smime", SMimeRootStore);
        }
    },
    async created() {
        const userRoles = await session.roles;
        if (userRoles.includes(BmRoles.CAN_USE_SMIME)) {
            this.$store.dispatch("smime/" + CHECK_IF_ASSOCIATED);
            navigator.serviceWorker?.addEventListener("controllerchange", () => {
                const isServiceWorkerAvailable = !!navigator.serviceWorker.controller;
                this.$store.commit("smime/" + SET_SW_AVAILABLE, isServiceWorkerAvailable);
                this.$store.dispatch("smime/" + CHECK_IF_ASSOCIATED);
            });
        }
    },
    destroyed() {
        if (this.$store.hasModule("smime")) {
            this.$store.unregisterModule("smime");
        }
    },
    render() {
        return "";
    }
};
</script>
