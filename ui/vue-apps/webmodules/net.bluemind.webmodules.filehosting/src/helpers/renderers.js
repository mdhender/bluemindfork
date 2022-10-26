import Vue from "vue";
import { BmTooLargeBox } from "@bluemind/styleguide";
import FhConfirmBox from "~/components/ConfirmBox";
import FhMustDetachConfirmBox from "~/components/MustDetachConfirmBox";
import FileHostingModal from "~/components/FileHostingModal";
import ComposerLinksWithFrame from "~/components/ComposerLinks/ComposerLinksWithFrame";
import ComposerLinks from "~/components/ComposerLinks/ComposerLinks";
const ComposerLinksWithFrameClass = Vue.extend(ComposerLinksWithFrame);
const ComposerLinksClass = Vue.extend(ComposerLinks);

export const LINKS_CLASSNAME = "filehosting-links";

export function renderMustDetachConfirmBox(vm, files, sizeLimit, message) {
    const content = vm.$createElement(FhMustDetachConfirmBox, {
        props: {
            files: files.map(file => {
                return {
                    name: file.name,
                    progress: { total: file.size, loaded: 0 }
                };
            }),
            sizeLimit,
            allFilesCount: message.attachments?.length + files.length
        }
    });
    const props = {
        title: vm.$tc("filehosting.add.large", files.length),
        okTitle: vm.$tc("filehosting.share.start", files.length),
        cancelTitle: vm.$t("common.cancel"),
        bodyClass: "pb-4",
        okVariant: "fill-accent",
        cancelVariant: "text",
        centered: true,
        hideHeaderClose: false
    };

    return { content, props };
}
export function renderShouldDetachConfirmBox(vm, files) {
    const content = vm.$createElement(FhConfirmBox, {
        props: {
            files: files.map(file => {
                return {
                    name: file.name,
                    progress: { total: file.size, loaded: 0 }
                };
            })
        },
        scopedSlots: {
            text: () =>
                vm.$createElement("span", [
                    vm.$tc("filehosting.threshold.almost_hit", files.length),
                    vm.$createElement("br"),
                    vm.$tc("filehosting.share.start", files.length),
                    " ?"
                ])
        }
    });
    const props = {
        title: vm.$tc("filehosting.add.large", files.length),
        okTitle: vm.$tc("filehosting.share.start", files.length),
        cancelTitle: vm.$t("mail.actions.attach"), //TODO: use a better wording
        bodyClass: "pb-4",
        okVariant: "fill-accent",
        cancelVariant: "text",
        centered: true,
        hideHeaderClose: false
    };

    return { content, props };
}

export function renderFileHostingModal(vm, message) {
    return {
        content: FileHostingModal,
        props: {
            sizeLimit: vm.$store.state.mail.messageCompose.maxMessageSize,
            message,
            centered: true
        }
    };
}
export function renderLinksWithFrameComponent(vm, files) {
    // This Class is a subclass of the Vue component. The parent property establishes a parent-child
    // relationship to current vm. This way this component can use its parent plugins like i18n.
    return new ComposerLinksWithFrameClass({
        parent: vm,
        propsData: {
            files,
            className: LINKS_CLASSNAME
        }
    });
}

export function renderLinksComponent(vm, props) {
    // This Class is a subclass of the Vue component. The parent property establishes a parent-child
    // relationship to current vm. This way this component can use its parent plugins like i18n.
    return new ComposerLinksClass({
        parent: vm,
        propsData: props
    });
}

export async function renderTooLargeFilesModal(vm, files, sizeLimit) {
    const content = vm.$createElement(BmTooLargeBox, {
        props: { sizeLimit, attachmentsCount: files.length },
        scopedSlots: { default: () => vm.$tc("filehosting.threshold.some_hit") }
    });

    const props = {
        title: vm.$tc("mail.actions.attach.too_large", files.length),
        okTitle: vm.$tc("common.got_it"),
        bodyClass: "pb-4",
        okVariant: "outline",
        centered: true
    };

    await vm.$bvModal.msgBoxOk([content], props);
}
