<template>
    <div
        :class="computeClassNames"
        class="mail-folder-input flex-fill d-none d-lg-flex align-items-center position-relative"
    >
        <bm-icon :icon="computeIconName" fixed-width />
        <bm-form-input
            ref="input"
            v-model="newFolderName"
            type="text"
            class="d-inline-block flex-fill"
            resettable
            :placeholder="folder ? '' : $t('mail.folder.new.from_scratch')"
            :state="isInputValid"
            aria-describedby="mail-folder-input-invalid"
            @focus="isActive = true"
            @focusout="onFocusOut"
            @keydown.enter="submit"
            @keydown.esc="closeInput"
            @reset="closeInput"
        />
        <bm-notice
            v-if="isNewFolderNameValid !== true"
            id="mail-folder-input-invalid"
            :text="isNewFolderNameValid"
            class="position-absolute z-index-110 mx-2"
        />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import { BmFormInput, BmIcon, BmNotice } from "@bluemind/styleguide";
import { isNameValid, normalize } from "~/model/folder";
import { FOLDERS_BY_UPPERCASE_PATH } from "~/getters";

export default {
    name: "MailFolderInput",
    components: {
        BmFormInput,
        BmIcon,
        BmNotice
    },
    props: {
        mailboxKey: {
            type: String,
            required: true
        },
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
        ...mapGetters("mail", { FOLDERS_BY_UPPERCASE_PATH }),
        isNewFolderNameValid() {
            if ((this.folder && this.folder.name === this.newFolderName) || this.newFolderName === "") {
                return true;
            }
            return isNameValid(this.newFolderName, this.path, this.FOLDERS_BY_UPPERCASE_PATH(this.mailboxKey));
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
            return this.isActive ? (this.shared ? "plus-folder-shared" : "plus-folder") : "plus-folder";
        },
        computeClassNames() {
            if (!this.isActive) {
                return "border-bottom";
            }
            return this.isNewFolderNameValid === true
                ? "valid border-bottom border-primary"
                : "invalid border-bottom border-danger";
        },
        isInputValid() {
            if (!this.newFolderName) {
                return null;
            }
            return this.isNewFolderNameValid === true;
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
                const normalizedName = normalize(this.newFolderName, this.FOLDERS_BY_UPPERCASE_PATH(this.mailboxKey));
                this.$emit("submit", normalizedName);
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
