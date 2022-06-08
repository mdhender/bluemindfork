import Vue from "vue";
import FhConfirmBox from "~/components/ConfirmBox";
import FhMustDetachConfirmBox from "~/components/MustDetachConfirmBox";
import FileHostingModal from "~/components/FileHostingModal";
import ComposerLinks from "~/components/ComposerLinks";
const ComposerLinksClass = Vue.extend(ComposerLinks);

export const LINKS_CLASSNAME = "filehosting-links";

export function renderMustDetachConfirmBox(vm, files, sizeLimit, message) {
    const content = vm.$createElement(FhMustDetachConfirmBox, {
        props: {
            attachments: files.map(file => {
                return {
                    fileName: file.name,
                    progress: { total: file.size, loaded: 0 }
                };
            }),
            sizeLimit,
            allAttachmentsCount: message.attachments?.length + files.length
        }
    });
    const props = {
        title: vm.$tc("mail.filehosting.add.large", files.length),
        okTitle: vm.$tc("mail.filehosting.share.start", files.length),
        cancelTitle: vm.$t("common.cancel"),
        bodyClass: "pb-4",
        cancelVariant: "simple-dark"
    };

    return { content, props };
}
export function renderShouldDetachConfirmBox(vm, files) {
    const content = vm.$createElement(FhConfirmBox, {
        props: {
            attachments: files.map(file => {
                return {
                    fileName: file.name,
                    progress: { total: file.size, loaded: 0 }
                };
            })
        },
        scopedSlots: {
            text: () =>
                vm.$createElement("span", [
                    vm.$tc("mail.filehosting.threshold.almost_hit", files.length),
                    vm.$createElement("br"),
                    vm.$tc("mail.filehosting.share.start", files.length),
                    " ?"
                ])
        }
    });
    const props = {
        title: vm.$tc("mail.filehosting.add.large", files.length),
        okTitle: vm.$tc("mail.filehosting.share.start", files.length),
        cancelTitle: vm.$t("mail.actions.attach"), //TODO: use a better wording
        bodyClass: "pb-4",
        cancelVariant: "simple-dark"
    };

    return { content, props };
}

export function renderFileHostingModal(vm, message) {
    return {
        content: FileHostingModal,
        props: {
            sizeLimit: vm.$store.state.mail.messageCompose.maxMessageSize,
            message
        }
    };
}
export function renderLinksComponent(vm, attachments) {
    // This Class is a subclass of the Vue component. The parent property establishes a parent-child
    // relationship to current vm. This way this component can use its parent plugins like i18n.
    return new ComposerLinksClass({
        parent: vm,
        propsData: {
            attachments,
            className: LINKS_CLASSNAME
        }
    });
}
