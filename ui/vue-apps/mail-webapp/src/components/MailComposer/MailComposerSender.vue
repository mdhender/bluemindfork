<template>
    <div class="mail-composer-sender ml-3 d-flex flex-column justify-content-between">
        <div class="d-flex align-items-center flex-fill">
            <span class="ml-2">{{ $t("common.from") }}</span>
            <bm-form-select
                :value="{ email: message.from.address, displayname: message.from.dn }"
                :options="options"
                class="ml-2 flex-fill"
                variant="inline-secondary"
                @input="changeIdentity"
            />
        </div>
        <hr class="m-0" />
    </div>
</template>

<script>
import { mapGetters, mapMutations, mapState } from "vuex";
import { inject } from "@bluemind/inject";
import { BmFormSelect } from "@bluemind/styleguide";
import { FOLDERS, MAILBOX_SENT } from "~/getters";
import { SET_MESSAGE_FROM, SET_MESSAGE_HEADERS } from "~/mutations";
import { MessageHeader } from "~/model/message";
import { DEFAULT_FOLDER_NAMES } from "~/store/folders/helpers/DefaultFolders";

export default {
    name: "MailComposerSender",
    components: { BmFormSelect },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("root-app", ["identities"]),
        ...mapState("mail", ["mailboxes"]),
        ...mapState("mail", { messages: ({ conversations }) => conversations.messages }),
        ...mapGetters("mail", { FOLDERS, MAILBOX_SENT }),
        options() {
            return this.identities.map(i => ({
                text: i.displayname ? `${i.displayname} <${i.email}>` : i.email,
                value: { email: i.email, displayname: i.displayname }
            }));
        }
    },
    methods: {
        ...mapMutations("mail", { SET_MESSAGE_FROM, SET_MESSAGE_HEADERS }),
        async changeIdentity(identity) {
            this.SET_MESSAGE_FROM({
                messageKey: this.message.key,
                from: { address: identity.email, dn: identity.displayname }
            });
            const fullIdentity = this.identities.find(
                i => i.email === identity.email && i.displayname === identity.displayname
            );
            const rawIdentity = await inject("UserMailIdentitiesPersistence").get(fullIdentity.id);
            if (rawIdentity.sentFolder !== DEFAULT_FOLDER_NAMES.SENT) {
                const mailbox =
                    this.mailboxes[`user.${rawIdentity.mailboxUid}`] || this.mailboxes[rawIdentity.mailboxUid];
                const sentFolder = this.MAILBOX_SENT(mailbox);
                if (sentFolder) {
                    const headers = this.messages[this.message.key]?.headers || [];
                    const xBmSentFolder = { name: MessageHeader.X_BM_SENT_FOLDER, values: [sentFolder.remoteRef.uid] };
                    this.SET_MESSAGE_HEADERS({ messageKey: this.message.key, headers: [...headers, xBmSentFolder] });
                }
            }
            this.$emit("update");
        }
    }
};
</script>
