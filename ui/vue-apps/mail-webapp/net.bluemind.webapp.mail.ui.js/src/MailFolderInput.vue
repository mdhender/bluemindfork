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
import { isFolderNameValid, isFolderPathTooLong } from "@bluemind/backend.mail.store";
import { mapGetters, mapState } from "vuex";
import ItemUri from "@bluemind/item-uri";

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
        ...mapGetters("mail-webapp", ["mailshares", "my"]),
        ...mapState("mail-webapp", ["currentFolderKey"]),
        isNewFolderNameValid() {
            if (this.folder && this.folder.name === this.newFolderName) {
                return true;
            }
            if (this.newFolderName !== "") {
                const currentMailbox = (this.folder && ItemUri.container(this.folder.key)) || this.my.mailboxUid;

                if (isFolderPathTooLong(this.folder, this.newFolderName)) {
                    return this.$t("mail.actions.folder.invalid.too_long");
                }

                const checkValidity = isFolderNameValid(this.newFolderName.toLowerCase());
                if (checkValidity !== true) {
                    return this.$t("mail.actions.folder.invalid.character", {
                        character: checkValidity
                    });
                }

                let path;
                if (this.folder) {
                    path =
                        this.folder.fullName.substring(0, this.folder.fullName.lastIndexOf("/") + 1) +
                        this.newFolderName;
                    if (this.my.mailboxUid !== currentMailbox) {
                        path = this.mailshares.find(mailshare => mailshare.uid === currentMailbox).name + "/" + path;
                    }
                } else {
                    path = this.newFolderName;
                }
                if (this.getFolderByPath(path, currentMailbox)) {
                    return this.$t("mail.actions.folder.invalid.already_exist");
                }
            }
            return true;
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
                const newName = this.newFolderName;
                this.closeInput();
                if (this.folder && this.folder.name === newName) {
                    return;
                }
                this.$emit("submit", newName);
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
