import { inject } from "@bluemind/inject";
import { mapActions, mapGetters, mapMutations } from "vuex";
import { loadingStatusUtils, mailboxUtils } from "@bluemind/mail";
import { MAILBOXES_ARE_LOADED, MAILBOX_BY_NAME, MY_MAILBOX, MY_MAILBOX_FOLDERS, MY_INBOX, MAILBOXES } from "~/getters";
import { FETCH_FOLDERS, FETCH_MAILBOXES, LOAD_MAX_MESSAGE_SIZE, UNREAD_FOLDER_COUNT } from "~/actions";
import { ADD_MAILBOXES } from "~/mutations";

const { LoadingStatus } = loadingStatusUtils;
const { create, MailboxType } = mailboxUtils;

export default {
    computed: {
        ...mapGetters("mail", {
            MAILBOX_BY_NAME,
            MAILBOXES_ARE_LOADED,
            MAILBOXES,
            MY_INBOX,
            MY_MAILBOX_FOLDERS,
            MY_MAILBOX
        })
    },
    methods: {
        ...mapActions("mail", { LOAD_MAX_MESSAGE_SIZE, FETCH_FOLDERS, FETCH_MAILBOXES, UNREAD_FOLDER_COUNT }),
        ...mapMutations("mail", { ADD_MAILBOXES }),
        ...mapMutations("root-app", ["SET_APP_STATE"]),
        $_BootstrapMixin_initMyMailbox() {
            const { userId: owner, defaultEmail: address } = inject("UserSession");
            const myMailbox = create({ owner, dn: address, address, type: MailboxType.USER });
            this.ADD_MAILBOXES([myMailbox]);
        },
        async $_BootstrapMixin_loadMailbox() {
            await this.FETCH_MAILBOXES();
            const mailbox = this.MAILBOX_BY_NAME(this.route.mailbox);
            await this.FETCH_FOLDERS({ mailbox });
        },
        async $_BootstrapMixin_loadAllMailboxes() {
            if (!this.MAILBOXES_ARE_LOADED) {
                await this.FETCH_MAILBOXES();
            }
            for (let mailbox of this.MAILBOXES) {
                if (mailbox.loading === LoadingStatus.NOT_LOADED) {
                    await this.FETCH_FOLDERS({ mailbox });
                }
            }
        }
    },

    async created() {
        try {
            this.$_BootstrapMixin_initMyMailbox();
            if (this.route.mailbox) {
                await this.$_BootstrapMixin_loadMailbox();
            }
            await this.FETCH_FOLDERS({ mailbox: this.MY_MAILBOX });
            await this.LOAD_MAX_MESSAGE_SIZE(inject("UserSession").userId);
            if (this.MY_INBOX?.unread === undefined) {
                await this.UNREAD_FOLDER_COUNT(this.MY_INBOX);
            }
            await this.$_BootstrapMixin_loadAllMailboxes();
        } catch (error) {
            // eslint-disable-next-line no-console
            console.error("Error while bootstraping application... ", error);
            this.SET_APP_STATE("error");
        }
    }
};
