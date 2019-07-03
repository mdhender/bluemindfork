<template>
    <bm-form class="mail-message-new mt-3 px-3">
        <bm-panel :title="panelTitle" @remove="close">
            <template #body>
                <bm-row class="align-items-center">
                    <bm-col cols="11">
                        <bm-contact-input ref="to" :contacts.sync="message_.to" class="mt-2">{{ $t("common.to") }}</bm-contact-input>
                    </bm-col>
                    <bm-col cols="1" class="text-center">
                        <bm-button
                            v-if="displayMode === 'reply'"
                            variant="link"
                            class="text-blue"
                            @click="expandAll = true"
                        >
                            <bm-icon icon="chevron" />
                        </bm-button>
                    </bm-col>
                </bm-row>
                <hr class="mt-0 mb-2" />

                <bm-row v-if="displayMode === 'expanded' || displayMode === 'default'">
                    <bm-col cols="11">
                        <bm-contact-input :contacts.sync="message_.cc">{{ $t("common.cc") }}</bm-contact-input>
                    </bm-col>
                    <bm-col cols="1" class="text-center">
                        <bm-button
                            v-if="!expandAll"
                            variant="link"
                            class="text-blue"
                            @click="expandAll = true"
                        >{{ $t("common.bcc") }}</bm-button>
                    </bm-col>
                </bm-row>
                <hr v-if="displayMode === 'expanded' || displayMode === 'default'" class="mt-0 mb-2" />

                <bm-contact-input v-if="displayMode === 'expanded'" :contacts.sync="message_.bcc">{{ $t("common.bcc") }}</bm-contact-input>
                <hr v-if="displayMode === 'expanded'" class="mt-0" />

                <bm-form-input
                    v-model="message_.subject"
                    :placeholder="$t('mail.new.subject.placeholder')"
                    :aria-label="$t('mail.new.subject.aria')"
                    type="text"
                    @keydown.enter.native.prevent
                />
                <bm-row class="d-block">
                    <hr class="bg-dark mt-1 mb-1" />
                </bm-row>
                <bm-form-group>
                    <bm-form-textarea
                        v-model="message_.content"
                        :rows="10"
                        :max-rows="10000"
                        :aria-label="$t('mail.new.content.aria')"
                        class="mail-content"
                        no-resize
                    />
                </bm-form-group>
                <bm-button
                    v-if="isAReply && !expandPreviousMessages"
                    variant="outline-dark"
                    class="pb-0"
                    @click="displayPreviousMessages"
                >
                    <bm-icon icon="3dots" size="sm" />
                </bm-button>
            </template>
            <template #footer>
                <mail-message-new-footer @save="save" @close="close" @send="send" />
            </template>
        </bm-panel>
    </bm-form>
</template>

<script>
import BmButton from "@bluemind/styleguide/components/buttons/BmButton";
import BmCol from "@bluemind/styleguide/components/layout/BmCol";
import BmContactInput from "@bluemind/styleguide/components/form/BmContactInput";
import BmFormTextarea from "@bluemind/styleguide/components/form/BmFormTextarea";
import BmFormInput from "@bluemind/styleguide/components/form/BmFormInput";
import BmForm from "@bluemind/styleguide/components/form/BmForm";
import BmFormGroup from "@bluemind/styleguide/components/form/BmFormGroup";
import BmIcon from "@bluemind/styleguide/components/BmIcon";
import BmPanel from "@bluemind/styleguide/components/BmPanel/BmPanel";
import BmRow from "@bluemind/styleguide/components/layout/BmRow";
import CommonL10N from "@bluemind/l10n";
import MailMessageNewFooter from "./MailMessageNewFooter";
import uuid from "uuid/v4";

export default {
    name: "MailMessageNew",
    components: {
        BmButton,
        BmCol,
        BmContactInput,
        BmFormTextarea,
        BmFormInput,
        BmForm,
        BmFormGroup,
        BmIcon,
        BmPanel,
        BmRow,
        MailMessageNewFooter
    },
    props: {
        message: {
            type: Object,
            default: () => null
        },
        mode: {
            type: String,
            default: null
        },
        previousMessage: {
            type: Object,
            default: null
        }
    },
    i18n: { messages: CommonL10N },
    data() {
        return {
            message_: {
                to: this.message ? this.message.to : [],
                cc: this.message ? this.message.cc : [],
                bcc: this.message ? this.message.bcc : [],
                subject: this.message ? this.message.subject : "",
                content: "",
                headers: []
            },
            expandAll: false,
            expandPreviousMessages: false
        };
    },
    computed: {
        panelTitle() {
            return this.message_.subject ? this.message_.subject : this.$t("mail.main.new");
        },
        isAReply() {
            return this.mode === "reply";
        },
        // inform which recipient fields must be displayed
        displayMode() {
            if (this.expandAll) {
                return "expanded"; // To & Cc & Bcc fields
            } else if (!this.expandAll && this.isAReply) {
                return "reply"; // only To field
            }
            return "default"; // To & Cc fields
        }
    },
    mounted: function() {
        this.$refs.to.focus();
    },
    methods: {
        displayPreviousMessages() {
            this.message_.content += "\n\n\n" + this.previousMessage.content;
            this.expandPreviousMessages = true;
        },
        send() {
            const messageToSend = JSON.parse(JSON.stringify(this.message_));
            if (this.isAReply && !this.expandPreviousMessages) {
                messageToSend.content += "\n\n\n" + this.previousMessage.content;
            }

            let outboxUid = this.$store.state["backend.mail/folders"].folders.find(function(folder) {
                return folder.displayName === "Outbox";
            }).uid;

            this.$store
                .dispatch("backend.mail/items/send", {
                    message: messageToSend,
                    isAReply: this.isAReply,
                    previousMessage: this.previousMessage,
                    outboxUid
                })
                .then(taskrefId => {
                    this.$store.commit("alert/addSuccess", {
                        uid: uuid(),
                        message: "Message successfully sent (" + taskrefId + ")"
                    });
                    this.close();
                })
                .catch(reason =>
                    this.$store.commit("alert/addError", { uid: uuid(), message: "Failed to send message " + reason })
                );
        },
        close() {
            if (this.mode === "reply") {
                let indexOfLastSlash = this.$store.state.route.path.lastIndexOf("/");
                let newPath = this.$route.path.substring(0, indexOfLastSlash);
                this.$router.push({ path: newPath });
            } else {
                this.$router.push({ path: "/mail/" });
            }
        },
        save() {
            // Not implemented yet
        }
    }
};
</script>

<style>
.mail-message-new input,
.mail-message-new textarea {
    border: none;
}

.mail-message-new input:focus,
.mail-message-new textarea:focus {
    box-shadow: none;
}

.mail-content {
    overflow: auto !important;
}
</style>
