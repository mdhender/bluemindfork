<template>
    <bm-form class="mail-composer p-lg-3 flex-grow-1 d-flex">
        <mail-composer-panel class="flex-grow-1">
            <template #header>
                <h3 class="d-none d-lg-flex text-nowrap text-truncate card-header px-2 py-1">
                    {{ panelTitle }}
                </h3>
            </template>
            <template #body>
                <div class="pl-3">
                    <bm-row class="align-items-center">
                        <bm-col cols="11">
                            <bm-contact-input
                                ref="to"
                                :contacts.sync="message.to"
                                :autocomplete-results="autocompleteResultsTo"
                                @search="searchedPattern => onSearch('to', searchedPattern)"
                                @update:contacts="saveDraft"
                            >
                                {{ $t("common.to") }}
                            </bm-contact-input>
                        </bm-col>
                        <bm-col cols="1" class="text-center">
                            <bm-button
                                v-if="displayedRecipientFields == recipientModes.TO"
                                variant="simple-dark"
                                class="text-blue"
                                @click="
                                    displayedRecipientFields =
                                        recipientModes.TO | recipientModes.CC | recipientModes.BCC
                                "
                            >
                                <bm-icon icon="chevron" />
                            </bm-button>
                        </bm-col>
                    </bm-row>
                    <hr class="m-0" />

                    <div v-if="displayedRecipientFields > recipientModes.TO" class="d-flex">
                        <div class="d-flex flex-grow-1">
                            <bm-contact-input
                                :contacts.sync="message.cc"
                                :autocomplete-results="autocompleteResultsCc"
                                class="w-100"
                                @search="searchedPattern => onSearch('cc', searchedPattern)"
                                @update:contacts="saveDraft"
                            >
                                {{ $t("common.cc") }}
                            </bm-contact-input>
                        </div>
                        <bm-button
                            v-if="displayedRecipientFields == (recipientModes.TO | recipientModes.CC)"
                            variant="simple-dark"
                            class="text-blue my-2 mr-1"
                            @click="
                                displayedRecipientFields = recipientModes.TO | recipientModes.CC | recipientModes.BCC
                            "
                        >
                            {{ $t("common.bcc") }}
                        </bm-button>
                    </div>
                    <hr v-if="displayedRecipientFields > recipientModes.TO" class="m-0" />

                    <bm-contact-input
                        v-if="displayedRecipientFields == (recipientModes.TO | recipientModes.CC | recipientModes.BCC)"
                        :contacts.sync="message.bcc"
                        :autocomplete-results="autocompleteResultsBcc"
                        @search="searchedPattern => onSearch('bcc', searchedPattern)"
                        @update:contacts="saveDraft"
                    >
                        {{ $t("common.bcc") }}
                    </bm-contact-input>
                    <hr
                        v-if="displayedRecipientFields == (recipientModes.TO | recipientModes.CC | recipientModes.BCC)"
                        class="m-0"
                    />

                    <bm-form-input
                        v-model="message.subject"
                        class="mail-composer-subject d-flex align-items-center"
                        :placeholder="$t('mail.new.subject.placeholder')"
                        :aria-label="$t('mail.new.subject.aria')"
                        type="text"
                        @keydown.enter.native.prevent
                        @input="saveDraft"
                    />
                </div>
                <bm-row class="d-block m-0"><hr class="bg-dark m-0" /></bm-row>
                <bm-row class="mt-1 mb-2">
                    <bm-col cols="12">
                        <bm-file-drop-zone
                            class="z-index-110 attachments"
                            file-type-regex="image/(jpeg|jpg|png|gif)"
                            @dropFiles="addAttachments($event)"
                        >
                            <template #dropZone>
                                <bm-icon icon="paper-clip" size="2x" />
                                <h2 class="text-center p-2">
                                    {{ $tc("mail.new.attachments.images.drop.zone", draggedFilesCount) }}
                                </h2>
                            </template>
                            <mail-attachments-block
                                v-if="message.attachments > 0"
                                :attachments="message.attachments"
                                editable
                                expanded
                            />
                        </bm-file-drop-zone>
                    </bm-col>
                </bm-row>
                <bm-file-drop-zone
                    class="z-index-110 as-attachments"
                    file-type-regex="^(?!.*image/(jpeg|jpg|png|gif)).*$"
                    at-least-one-match
                    @dropFiles="addAttachments($event)"
                    @filesCount="draggedFilesCount = $event"
                >
                    <template #dropZone>
                        <h2 class="text-center p-2">{{ $tc("mail.new.attachments.drop.zone", draggedFilesCount) }}</h2>
                        <bm-icon icon="arrow-up" size="2x" />
                    </template>
                    <bm-file-drop-zone class="z-index-110" inline file-type-regex="image/(jpeg|jpg|png|gif)">
                        <template #dropZone>
                            <bm-icon class="text-dark" icon="file-type-image" size="2x" />
                            <h2 class="text-center p-2">{{ $tc("mail.new.images.drop.zone", draggedFilesCount) }}</h2>
                        </template>
                        <bm-form-textarea
                            v-if="userPrefTextOnly"
                            ref="message-content"
                            v-model="editorContent"
                            :rows="10"
                            :max-rows="10000"
                            :aria-label="$t('mail.new.content.aria')"
                            class="mail-content"
                            no-resize
                            @input="saveDraft"
                        />
                        <bm-rich-editor
                            v-else
                            ref="message-content"
                            v-model="editorContent"
                            :is-menu-bar-opened="userPrefIsMenuBarOpened"
                            class="h-100"
                            @input="saveDraft"
                        >
                            <bm-button
                                v-if="collapsedContent"
                                variant="outline-dark"
                                class="align-self-start ml-3 mb-2"
                                @click="expandContent"
                            >
                                <bm-icon icon="3dots" size="sm" />
                            </bm-button>
                        </bm-rich-editor>
                    </bm-file-drop-zone>
                </bm-file-drop-zone>
                <bm-button
                    v-if="userPrefTextOnly && collapsedContent"
                    variant="outline-dark"
                    class="align-self-start"
                    @click="expandContent"
                >
                    <bm-icon icon="3dots" size="sm" />
                </bm-button>
            </template>
            <template #footer>
                <mail-composer-footer
                    :message="message"
                    :user-pref-text-only="userPrefTextOnly"
                    :user-pref-is-menu-bar-opened="userPrefIsMenuBarOpened"
                    @toggleTextFormat="userPrefIsMenuBarOpened = !userPrefIsMenuBarOpened"
                    @delete="deleteDraft"
                    @send="send"
                />
            </template>
        </mail-composer-panel>
    </bm-form>
</template>

<script>
import { mapGetters, mapActions, mapMutations, mapState } from "vuex";
import debounce from "lodash/debounce";

import { OrderBy } from "@bluemind/addressbook.api";
import { VCardInfoAdaptor } from "@bluemind/contact";
import { MimeType, PartsHelper } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import {
    BmButton,
    BmCol,
    BmContactInput,
    BmFormInput,
    BmForm,
    BmFormTextarea,
    BmIcon,
    BmRichEditor,
    BmRow,
    BmTooltip,
    BmFileDropZone
} from "@bluemind/styleguide";

import MailAttachmentsBlock from "../MailAttachment/MailAttachmentsBlock";
import MailComposerFooter from "./MailComposerFooter";
import MailComposerPanel from "./MailComposerPanel";

const recipientModes = { TO: 1, CC: 2, BCC: 4 }; // flags for the display mode of MailComposer's recipients fields

export default {
    name: "MailComposer",
    components: {
        BmButton,
        BmCol,
        BmContactInput,
        BmFileDropZone,
        BmFormInput,
        BmForm,
        BmFormTextarea,
        BmIcon,
        MailComposerPanel,
        BmRichEditor,
        BmRow,
        MailComposerFooter,
        MailAttachmentsBlock
    },
    directives: { BmTooltip },
    props: {
        messageKey: {
            type: String,
            default: undefined
        }
    },
    data() {
        return {
            message: {},
            recipientModes,
            autocompleteResults: [],
            autocompleteResultsTo: [],
            autocompleteResultsCc: [],
            autocompleteResultsBcc: [],
            debouncedSave: debounce(
                () =>
                    this.save({
                        message: this.message,
                        editorContent: this.editorContent,
                        userPrefTextOnly: this.userPrefTextOnly
                    }),
                1000
            ),
            userPrefIsMenuBarOpened: false, // TODO: initialize this with user setting
            userPrefTextOnly: false, // TODO: initialize this with user setting
            editorContent: "",
            collapsedContent: null, // FIXME: init if a separator is detected in content
            /**
             * @example
             * displayedRecipientFields = (TO|CC|BCC) means we want to display all 3 fields
             * displayedRecipientFields = TO means we want to display TO field only
             */
            displayedRecipientFields: recipientModes.TO | recipientModes.CC, // FIXME: init it if a separator is detected in content
            draggedFilesCount: -1
        };
    },
    computed: {
        ...mapGetters("mail-webapp", ["lastRecipients"]),
        ...mapState("mail", ["messages"]),
        panelTitle() {
            return this.message.subject ? this.message.subject : this.$t("mail.main.new");
        }
    },
    watch: {
        autocompleteResults: function () {
            this.autocompleteResultsTo = this.getAutocompleteResults("to");
            this.autocompleteResultsCc = this.getAutocompleteResults("cc");
            this.autocompleteResultsBcc = this.getAutocompleteResults("bcc");
        },
        messageKey: {
            handler: async function (old, newT) {
                console.log("MailCOMPOSER messageKey changed ! old: ", old, " / new : ", newT);
                this.message = { ...this.messages[this.messageKey] };
                await this.initEditorContent();
                console.log(this.message);
                if (this.message.to.length > 0 || this.message.cc.length > 0) {
                    this.$refs["message-content"].focus();
                } else {
                    this.$refs.to.focus();
                }
            },
            immediate: true
        }
    },
    created: function () {
        this.deleteAllSelectedMessages();
    },
    methods: {
        ...mapActions("mail-webapp", { save: "saveDraft", addAttachments: "addAttachments" }),
        ...mapMutations("mail-webapp", ["deleteAllSelectedMessages"]),
        async initEditorContent() {
            /**
             * FIXME (EN FAIRE UN TICKET)
             * Actually composer is very strict because it accepts only structure generated by himself : classic alternative case with text plain or html (inlined images are supported).
             * But it would be better if it supports draft coming from other clients. So we need to support a lot of cases :
             *      - if message got just a text plain part
             *      - if message contains only a html part and if userPrefTextOnly == true then we must convert HTML part into text part
             *      - if message contains only a plain text part and userPrefTextOnly == false then we must convert plain text part into HTML
             *      - all other complex cases are not supported yet.. (for example mixed part with an html followed by an image, followed by a text plain, etc)
             */

            let newContent;
            if (this.userPrefTextOnly) {
                const textPlainPart = this.message.inlinePartsByCapabilities.find(
                    part => part.capabilities === MimeType.TEXT_PLAIN
                ).parts[0];
                newContent = await fetchPart(textPlainPart, this.message);
            } else {
                let parts = this.message.inlinePartsByCapabilities.find(
                    part => part.capabilities[0] === MimeType.TEXT_HTML
                ).parts;
                const partsContent = await Promise.all(parts.map(part => fetchPart(part, this.message)));
                parts = parts.map((part, index) => ({ ...part, content: partsContent[index] }));
                const htmlPart = parts.find(part => part.mime === MimeType.TEXT_HTML);
                PartsHelper.insertInlineImages(
                    [htmlPart],
                    parts.filter(part => MimeType.isImage(part) && part.contentId)
                );
                newContent = htmlPart.content;
            }
            this.updateEditorContent(newContent);
        },
        async expandContent() {
            this.updateEditorContent(this.editorContent + this.collapsedContent);
            this.collapsedContent = null;
        },
        async updateEditorContent(newContent) {
            this.editorContent = newContent;
            await this.$nextTick();
            if (this.userPrefTextOnly) {
                this.$refs["message-content"].focus();
                this.$refs["message-content"].setSelectionRange(0, 0);
            } else {
                this.$refs["message-content"].updateContent();
                this.$refs["message-content"].focus("start");
            }
        },
        send() {
            this.debouncedSave.cancel();
            this.$store.dispatch("mail-webapp/send", {
                message: this.message,
                editorContent: this.editorContent,
                userPrefTextOnly: this.userPrefTextOnly
            });
            this.$router.navigate("v:mail:home");
        },
        saveDraft() {
            this.debouncedSave.cancel();
            this.debouncedSave();
        },
        async deleteDraft() {
            this.debouncedSave.cancel();
            if (this.isMessageEmpty()) {
                this.$store.dispatch("mail-webapp/deleteDraft").then(() => this.$router.navigate("v:mail:message"));
                return;
            }
            const confirm = await this.$bvModal.msgBoxConfirm(this.$t("mail.draft.delete.confirm.content"), {
                title: this.$t("mail.draft.delete.confirm.title"),
                okTitle: this.$t("common.delete"),
                cancelVariant: "outline-secondary",
                cancelTitle: this.$t("common.cancel"),
                centered: true,
                hideHeaderClose: false
            });
            if (confirm) {
                // delete the draft then close the composer
                this.$store.dispatch("mail-webapp/deleteDraft").then(() => this.$router.navigate("v:mail:message"));
            }
        },
        onSearch(fieldFocused, searchedPattern) {
            this.fieldFocused = fieldFocused;
            this.search(searchedPattern);
        },
        search: debounce(function (searchedRecipient) {
            if (searchedRecipient === "") {
                this.autocompleteResults = [];
            } else {
                return inject("AddressBooksPersistence")
                    .search({
                        from: 0,
                        size: 5,
                        query: searchedRecipient,
                        orderBy: OrderBy.Pertinance,
                        escapeQuery: false
                    })
                    .then(results => {
                        if (results.values.length === 0) {
                            this.autocompleteResults = undefined;
                        } else {
                            this.autocompleteResults = results.values.map(vcardInfo =>
                                VCardInfoAdaptor.toContact(vcardInfo)
                            );
                        }
                    });
            }
        }, 200),
        getAutocompleteResults(fromField) {
            if (fromField !== this.fieldFocused || this.autocompleteResults === undefined) {
                return [];
            }
            if (this.autocompleteResults.length > 0) {
                return this.autocompleteResults;
            } else {
                return this.lastRecipients;
            }
        }
    }
};

async function fetchPart(part, message) {
    const stream = await inject("MailboxItemsPersistence", message.folderRef.uid).fetch(
        message.remoteRef.imapUid,
        part.address,
        part.encoding,
        part.mime,
        part.charset
    );
    if (MimeType.isText(part) || MimeType.isHtml(part) || MimeType.isCalendar(part)) {
        return new Promise(resolve => {
            const reader = new FileReader();
            reader.readAsText(stream, part.encoding);
            reader.addEventListener("loadend", e => {
                resolve(e.target.result);
            });
        });
    }
    return stream;
}
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-composer {
    .mail-composer-subject input,
    .bm-contact-input input,
    textarea {
        border: none;
    }

    input:focus,
    textarea:focus {
        box-shadow: none;
    }

    .bm-rich-editor-content .ProseMirror {
        min-height: 12rem;
    }

    .mail-content {
        overflow: auto !important;
    }

    .ProseMirror,
    .mail-content {
        padding: $sp-2 $sp-3;
    }

    .mail-composer-subject {
        min-height: 2.5rem;
    }

    .bm-contact-input .btn {
        min-width: 3rem;
        text-align: left;
    }

    .bm-contact-input .bm-form-autocomplete-input .suggestions {
        z-index: 200;
    }

    .bm-file-drop-zone.attachments .bm-dropzone-active-content {
        min-height: 7em;
    }

    .bm-file-drop-zone.as-attachments.bm-dropzone-active,
    .bm-file-drop-zone.as-attachments.bm-dropzone-hover {
        background: url("~@bluemind/styleguide/assets/attachment.png") no-repeat center center;
    }

    .mail-composer-panel-footer {
        display: none;
    }

    @include media-breakpoint-up(lg) {
        .mail-composer-panel-footer {
            display: block;
        }
    }
}
</style>
