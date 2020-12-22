import { inject } from "@bluemind/inject";
import { mapActions, mapGetters, mapMutations } from "vuex";
import { FETCH_FOLDERS, FETCH_MAILBOXES, FETCH_SIGNATURE, BOOTSTRAP } from "~actions";
import { MAILSHARES, MY_MAILBOX, MY_MAILBOX_FOLDERS } from "~getters";
import { ADD_MAILBOXES } from "~mutations";
import { create, MailboxType } from "../model/mailbox";
import MessageQueryParam from "../router/MessageQueryParam";

export default {
    computed: {
        ...mapGetters("mail", { MY_MAILBOX, MY_MAILBOX_FOLDERS, MAILSHARES })
    },
    methods: {
        ...mapActions("mail", { BOOTSTRAP, FETCH_FOLDERS, FETCH_MAILBOXES, FETCH_SIGNATURE }),
        ...mapActions("mail-webapp", ["loadMessageList"]),
        ...mapMutations("mail", { ADD_MAILBOXES }),
        ...mapMutations("root-app", ["SET_APP_STATE"])
    },

    async created() {
        const { mailshare } = MessageQueryParam.parse(this.$route.params.messagequery);
        const { userId: owner, formatedName: name } = inject("UserSession");
        const myMailbox = create({ owner, name, type: MailboxType.USER });
        this.ADD_MAILBOXES([myMailbox]);

        try {
            await this.FETCH_FOLDERS(this.MY_MAILBOX);
            if (!mailshare) {
                await this.loadMessageList(MessageQueryParam.parse(this.$route.params.messagequery));
            }
            await this.FETCH_MAILBOXES();
            this.MAILSHARES.forEach(async mailbox => {
                await this.FETCH_FOLDERS(mailbox);
                if (mailshare && mailshare.split("/").shift() === mailshare.name) {
                    await this.loadMessageList(MessageQueryParam.parse(this.$route.params.messagequery));
                }
            });
            this.MY_MAILBOX_FOLDERS.forEach(({ key }) => this.$store.dispatch("mail-webapp/loadUnreadCount", key));
            this.FETCH_SIGNATURE();
            this.$store.dispatch("mail-webapp/loadMailboxConfig");
        } catch (e) {
            this.SET_APP_STATE("error");
        }
    }
};
