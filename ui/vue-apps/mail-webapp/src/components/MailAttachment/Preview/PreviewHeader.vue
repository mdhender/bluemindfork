<template>
    <bm-extension
        id="webapp.mail"
        v-slot="context"
        class="preview-header flex-row"
        path="message.file"
        type="renderless"
        :file="file"
    >
        <div class="preview-header-desktop desktop-only">
            <preview-message-header :expanded="expanded" @click.native="$emit('update:expanded', !expanded)" />
            <div class="main-part">
                <bm-button-toolbar class="px-5">
                    <bm-icon-button
                        :disabled="filesCount <= 1"
                        tab-index="0"
                        :title="$t('mail.preview.previous')"
                        icon="chevron-left"
                        @click="$emit('previous')"
                    />
                    <bm-icon-button
                        :disabled="filesCount <= 1"
                        :title="$t('mail.preview.next')"
                        icon="chevron-right"
                        @click="$emit('next')"
                    />
                </bm-button-toolbar>

                <preview-file-header :file="context.file" />

                <bm-button-toolbar>
                    <bm-icon-button
                        :disabled="!isPreviewable(context.file)"
                        :title="
                            $t('mail.content.print', {
                                fileType: $t('mail.content.' + matchingIcon),
                                name: file.name
                            })
                        "
                        icon="printer"
                        @click="print(file)"
                    />
                    <bm-icon-button
                        :href="file.url"
                        :download="file.name"
                        :title="
                            $t('mail.content.download', {
                                fileType: $t('mail.content.' + matchingIcon),
                                name: file.name
                            })
                        "
                        icon="download"
                    />
                    <bm-icon-button
                        :title="$t('mail.content.open-new-tab', { name: file.name })"
                        :disabled="!isPreviewable(context.file)"
                        icon="popup"
                        @click="open(context.file)"
                    />
                    <bm-button-close
                        size="lg"
                        class="ml-5"
                        :title="$t('common.close_window')"
                        @click="$emit('close')"
                    />
                </bm-button-toolbar>
            </div>
        </div>

        <bm-navbar class="preview-header-mobile mobile-only">
            <bm-navbar-back @click="$emit('close')" />
            <preview-file-header :file="context.file" />
            <bm-button-toolbar class="pl-3">
                <bm-icon-button
                    :disabled="filesCount <= 1"
                    variant="compact-on-fill-primary"
                    size="lg"
                    tab-index="0"
                    :title="$t('mail.preview.previous')"
                    icon="chevron-left"
                    @click="$emit('previous')"
                />
                <bm-icon-button
                    :disabled="filesCount <= 1"
                    variant="compact-on-fill-primary"
                    size="lg"
                    class="ml-3"
                    :title="$t('mail.preview.next')"
                    icon="chevron-right"
                    @click="$emit('next')"
                />
                <bm-icon-dropdown
                    variant="compact-on-fill-primary"
                    size="lg"
                    no-caret
                    class="ml-5 mr-3"
                    :title="$t('mail.actions.other')"
                    icon="3dots-v"
                >
                    <bm-dropdown-item :disabled="!isPreviewable(context.file)" icon="printer" @click="print(file)">{{
                        $t("mail.content.print", {
                            fileType: $t("mail.content." + matchingIcon),
                            name: file.name
                        })
                    }}</bm-dropdown-item>
                    <bm-dropdown-item :href="file.url" :download="file.name" icon="download">{{
                        $t("mail.content.download", {
                            fileType: $t("mail.content." + matchingIcon),
                            name: file.name
                        })
                    }}</bm-dropdown-item>
                    <bm-dropdown-item :disabled="!isPreviewable(context.file)" icon="popup" @click="open(context.file)">
                        {{ $t("mail.content.open-new-tab", { name: file.name }) }}
                    </bm-dropdown-item>
                </bm-icon-dropdown>
            </bm-button-toolbar>
        </bm-navbar>
    </bm-extension>
</template>

<script>
import {
    BmButtonClose,
    BmButtonToolbar,
    BmDropdownItem,
    BmIconButton,
    BmIconDropdown,
    BmNavbar,
    BmNavbarBack
} from "@bluemind/ui-components";
import { MimeType } from "@bluemind/email";
import { BmExtension } from "@bluemind/extensions.vue";
import PreviewFileHeader from "./PreviewFileHeader";
import PreviewMessageHeader from "./PreviewMessageHeader";
import { fileUtils } from "@bluemind/mail";
const { isAllowedToPreview, hasRemoteContent } = fileUtils;

export default {
    name: "PreviewHeader",
    components: {
        BmExtension,
        PreviewMessageHeader,
        PreviewFileHeader,
        BmButtonClose,
        BmButtonToolbar,
        BmDropdownItem,
        BmIconButton,
        BmIconDropdown,
        BmNavbar,
        BmNavbarBack
    },
    props: {
        file: {
            type: Object,
            required: true
        },
        filesCount: {
            type: Number,
            required: true
        },
        expanded: { type: Boolean, required: true }
    },
    computed: {
        matchingIcon() {
            return MimeType.matchingIcon(this.file.mime);
        },
        blockedRemoteContent() {
            return this.$store.state.mail.consultPanel.remoteImages.mustBeBlocked;
        }
    },
    methods: {
        open(file) {
            window.open(file.url);
        },
        print(file) {
            const win = window.open(file.url);
            win.addEventListener("afterprint", () => win.close());
            win.addEventListener("load", () => win.print());
        },
        isPreviewable(file) {
            return isAllowedToPreview(file) && !(hasRemoteContent(file) && this.blockedRemoteContent);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.preview-header .preview-header-desktop {
    height: base-px-to-rem(40);
    background-color: $surface;
    display: flex;

    .preview-message-header {
        flex: none;
        width: 25%;
    }

    .main-part {
        background-color: $neutral-bg;
        flex: 1;
        display: flex;

        > .preview-file-header {
            flex: 1;
        }
        > .btn-toolbar {
            flex: none;
            align-items: center;
            .bm-button-close {
                margin-right: $sp-4;
            }
        }
    }
}

.preview-header .preview-header-mobile {
    .btn-toolbar {
        flex: none;
    }
}
</style>
