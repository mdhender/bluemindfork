import BoostrapMixin from "./BootstrapMixin";
import RouterMixin from "./RouterMixin";
import ServerPush from "./ServerPush";
import MailAppL10N from "../../../l10n/";
import { SET_MAIL_THREAD_SETTING } from "~/mutations";

export default {
    mixins: [BoostrapMixin, RouterMixin, ServerPush],
    componentI18N: { messages: MailAppL10N },
    created() {
        this.$store.commit(`mail/${SET_MAIL_THREAD_SETTING}`, this.$store.state.settings.mail_thread);
    }
};
