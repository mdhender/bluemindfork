<template>
    <div class="mail-folder-input flex-fill d-none d-lg-flex align-items-center position-relative">
        <bm-form-input
            ref="input"
            v-model="newFolderName"
            type="text"
            class="d-inline-block flex-fill"
            variant="underline"
            icon="plus"
            left-icon
            resettable
            :placeholder="folder ? '' : $t('mail.folder.new.from_scratch')"
            :state="inputState"
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
import { BmFormInput, BmNotice } from "@bluemind/styleguide";
import { isNameValid, normalize } from "~/model/folder";
import { FOLDER_BY_PATH, FOLDERS_BY_PATH, MY_MAILBOX } from "~/getters";

export default {
    name: "MailFolderInput",
    components: {
        BmFormInput,
        BmNotice
    },
    props: {
        mailboxKey: {
            type: String,
            default: null
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
        ...mapGetters("mail", { FOLDER_BY_PATH, FOLDERS_BY_PATH, MY_MAILBOX }),
        isNewFolderNameValid() {
            if ((this.folder && this.folder.name === this.newFolderName) || this.newFolderName === "") {
                return true;
            }
            return isNameValid(this.newFolderName, this.path, this.folderByPath());
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
        inputState() {
            if (!this.newFolderName) return null;
            if (this.isNewFolderNameValid === false) return false;
            return null;
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
        folderByPath() {
            const mailboxKey = this.mailboxKey || this.folder?.mailboxRef.key;
            return mailboxKey
                ? path => this.FOLDER_BY_PATH(path, { key: mailboxKey })
                : path =>
                      this.FOLDERS_BY_PATH(path).find(
                          ({ key }, index, arr) => key === this.MY_MAILBOX.key || index === arr.length - 1
                      );
        },
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
                const normalizedName = normalize(this.newFolderName, this.folderByPath());
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
        },
        focus() {
            this.$refs["input"].focus();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";
.mail-folder-input {
    .bm-form-input input:not(:focus) {
        border-bottom-color: $light;
    }
    .bm-notice {
        top: 30px;
        left: 0px;
        width: calc(100% - 2 * #{$sp-2});
    }
}
</style>
