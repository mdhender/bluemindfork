import BoostrapMixin from "./BootstrapMixin";
import RouterMixin from "./RouterMixin";
import ServerPush from "./ServerPush";
import { SET_MAIL_THREAD_SETTING } from "~/mutations";

export default {
    mixins: [BoostrapMixin, RouterMixin, ServerPush],
    created() {
        this.$store.commit(`mail/${SET_MAIL_THREAD_SETTING}`, this.$store.state.settings.mail_thread);
    }
};
