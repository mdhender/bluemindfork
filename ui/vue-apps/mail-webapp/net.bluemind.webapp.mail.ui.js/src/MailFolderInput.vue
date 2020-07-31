<template>
    <div :class="computeClassNames" class="mail-folder-input flex-fill d-flex align-items-center position-relative">
        <bm-icon :icon="computeIconName" fixed-width />
        <bm-form-input
            ref="input"
            v-model="newFolderName"
            type="text"
            class="d-inline-block flex-fill"
            reset
            :placeholder="folder ? '' : $t('mail.folder.new.from_scratch')"
            @focus="isActive = true"
            @focusout="onFocusOut"
            @keydown.enter="submit"
            @keydown.esc="closeInput"
            @reset="closeInput"
        />
        <bm-notice
            v-if="isNewFolderNameValid !== true"
            :text="isNewFolderNameValid"
            class="position-absolute z-index-110 mx-2"
        />
    </div>
</template>

<script>
import { BmFormInput, BmIcon, BmNotice } from "@bluemind/styleguide";
import { isFolderNameValid } from "@bluemind/backend.mail.store";
import { mapGetters } from "vuex";

const FOLDER_PATH_MAX_LENGTH = 250;

export default {
    name: "MailFolderInput",
    components: {
        BmFormInput,
        BmIcon,
        BmNotice
    },
    props: {
        folder: {
            type: Object,
            required: false,
            default: null
        },
        shared: {
            type: Boolean,
            required: false,
            default: false
        },
        submitOnFocusout: {
            type: Boolean,
            required: false,
            default: true
        }
    },
    data() {
        return {
            newFolderName: (this.folder && this.folder.name) || "",
            isActive: false
        };
    },
    computed: {
        ...mapGetters("mail-webapp/folders", ["getFolderByPath"]),
        ...mapGetters("mail-webapp", ["mailshares"]),
        ...mapGetters("mail", { myMailboxKey: "MY_MAILBOX_KEY" }),
        isNewFolderNameValid() {
            if (this.folder && this.folder.name === this.newFolderName) {
                return true;
            }
            if (this.newFolderName !== "") {
                const currentMailbox = (this.folder && this.folder.mailbox) || this.myMailboxKey;

                if (this.path.length > FOLDER_PATH_MAX_LENGTH) {
                    return this.$t("mail.actions.folder.invalid.too_long");
                }

                const checkValidity = isFolderNameValid(this.newFolderName.toLowerCase());
                if (checkValidity !== true) {
                    return this.$t("mail.actions.folder.invalid.character", {
                        character: checkValidity
                    });
                }

                if (this.getFolderByPath(this.path, currentMailbox)) {
                    return this.$t("mail.actions.folder.invalid.already_exist");
                }
            }
            return true;
        },
        path() {
            let path = "";
            if (this.isRename) {
                const splitted = this.folder.path.split("/");
                splitted.pop();
                path = splitted ? splitted + "/" : "";
            } else if (this.folder) {
                path = this.folder.path + "/";
            }
            path += this.newFolderName;
            return path;
        },
        isRename() {
            return this.folder && this.folder.name !== "";
        },
        computeIconName() {
            return this.isActive ? (this.shared ? "folder-shared" : "folder") : "plus";
        },
        computeClassNames() {
            if (!this.isActive) {
                return "border-bottom";
            }
            return this.isNewFolderNameValid === true
                ? "valid border-bottom border-primary"
                : "invalid border-bottom border-danger";
        }
    },
    watch: {
        folder() {
            this.newFolderName = this.folder.name;
        },
        newFolderName() {
            if (this.newFolderName !== "" && !this.isActive) {
                this.isActive = true;
            }
        }
    },
    methods: {
        closeInput() {
            this.$emit("close");
            this.isActive = false;
            this.newFolderName = (this.folder && this.folder.name) || "";
        },
        submit() {
            if (this.isNewFolderNameValid === true && this.newFolderName !== "") {
                if (this.folder && this.folder.name === this.newFolderName) {
                    return;
                }
                this.$emit("submit", this.newFolderName);
                this.closeInput();
            }
        },
        onFocusOut() {
            if (
                !this.$el.contains(document.activeElement) &&
                !this.$el.contains(event.relatedTarget) &&
                this.isActive
            ) {
                if (this.isNewFolderNameValid !== true || this.newFolderName === "" || !this.submitOnFocusout) {
                    this.closeInput();
                } else {
                    this.submit();
                }
            }
        },
        select() {
            this.$refs["input"].select();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-folder-input {
    &.valid .fa-folder,
    &.valid .fa-folder-shared {
        color: $primary;
    }

    &.invalid {
        .fa-folder,
        .fa-folder-shared,
        input {
            color: $danger;
        }
    }

    input {
        border: none !important;
        background-color: transparent !important;
        box-shadow: none !important;
        padding-left: $sp-1 !important;
    }

    .bm-notice {
        top: 30px;
        left: 0px;
        width: calc(100% - 2 * #{$sp-2});
    }

    .fa-plus {
        color: $secondary;
    }
}
</style>
