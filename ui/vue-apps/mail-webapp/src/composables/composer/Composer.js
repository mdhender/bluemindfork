import cloneDeep from "lodash.clonedeep";
import { ERROR, REMOVE } from "@bluemind/alert.store";
import { draftUtils, messageUtils } from "@bluemind/mail";
import { DEBOUNCED_SAVE_MESSAGE, REQUEST_DSN, TOGGLE_DSN_REQUEST } from "~/actions";
import { RESET_COMPOSER, SET_MESSAGE_HEADERS, SET_SAVE_ERROR } from "~/mutations";
import { useGetMailTipsCommand } from "~/commands";
import { IS_SENDER_SHOWN } from "~/getters";
import { Flag } from "@bluemind/email";
import store from "@bluemind/store";
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { setFrom, setIdentity } from "./ComposerFrom";

const maxMessageSizeExceededAlert = {
    alert: { name: "mail.DRAFT_EXCEEDS_MAX_MESSAGE_SIZE", uid: "DRAFT_EXCEEDS_MAX_MESSAGE_SIZE" },
    options: { area: "right-panel", renderer: "DraftExceedsMaxMessageSizeAlert" }
};

export function useComposer(message, contentRef) {
    const execGetMailTipsCommand = useGetMailTipsCommand();

    const draggedFilesCount = ref(-1);
    const isSignatureInserted = ref(false);

    const isSenderShown = computed(() => store.getters[`mail/${IS_SENDER_SHOWN}`](store.state.settings));
    const isDeliveryStatusRequested = computed(() => message.value.flags.includes(Flag.BM_DSN));
    const isDispositionNotificationRequested = computed(
        () => messageUtils.findDispositionNotificationHeaderIndex(message.value.headers) >= 0
    );
    const messageCompose = computed(() => store.state.mail.messageCompose);

    watch(
        () => store.state.mail.messageCompose.maxMessageSizeExceeded,
        hasExceeded => {
            if (hasExceeded) {
                store.dispatch(`alert/${ERROR}`, maxMessageSizeExceededAlert);
            } else {
                store.dispatch(`alert/${REMOVE}`, maxMessageSizeExceededAlert.alert);
            }
        },
        { immediate: true }
    );

    watch(
        () => message.value.from,
        value => {
            if (isDispositionNotificationRequested.value) {
                setDispositionNotificationHeader(value);
            }
        },
        { immediate: true }
    );

    onMounted(() => {
        store.commit("mail/" + SET_SAVE_ERROR, {});
        if (message.value.from) {
            setIdentity({ email: message.value.from.address, displayname: message.value.from.dn });
        }
        if (draftUtils.isNewMessage(message.value)) {
            if (store.state.settings.always_ask_read_receipt === "true") {
                setDispositionNotificationHeader(message.value.from);
            }
            if (store.state.settings.always_ask_delivery_receipt === "true") {
                store.dispatch(`mail/${REQUEST_DSN}`, message.value);
            }
        }
    });

    onUnmounted(() => {
        store.commit("mail/" + RESET_COMPOSER);
    });

    function toggleSignature() {
        contentRef.value?.toggleSignature();
    }

    function toggleDeliveryStatus() {
        store.dispatch(`mail/${TOGGLE_DSN_REQUEST}`, message.value);
        store.dispatch(`mail/${DEBOUNCED_SAVE_MESSAGE}`, {
            draft: message.value,
            messageCompose: cloneDeep(store.state.mail.messageCompose)
        });
    }

    function toggleDispositionNotification() {
        const headers = [...message.value.headers];
        if (isDispositionNotificationRequested.value) {
            messageUtils.removeDispositionNotificationHeader(headers);
        } else {
            messageUtils.setDispositionNotificationHeader(headers, message.value.from);
        }
        store.commit(`mail/${SET_MESSAGE_HEADERS}`, { messageKey: message.value.key, headers });
        store.dispatch(`mail/${DEBOUNCED_SAVE_MESSAGE}`, {
            draft: message.value,
            messageCompose: cloneDeep(store.state.mail.messageCompose)
        });
    }

    async function checkAndRepairFrom() {
        const matchingIdentity = store.state["root-app"].identities.find(
            i => i.email === message.value.from.address && i.displayname === message.value.from.dn
        );
        if (!matchingIdentity) {
            // eslint-disable-next-line no-console
            console.warn("identity changed because no identity matched message.from");
            const defaultIdentity = store.getters["root-app/DEFAULT_IDENTITY"];
            await setFrom(defaultIdentity, message.value);
        }
    }

    function setDispositionNotificationHeader(from) {
        const headers = [...message.value.headers];
        messageUtils.setDispositionNotificationHeader(headers, from);
        store.commit("mail/" + SET_MESSAGE_HEADERS, { messageKey: message.value.key, headers });
    }

    return {
        draggedFilesCount,
        isSignatureInserted,
        isSenderShown,
        isDeliveryStatusRequested,
        isDispositionNotificationRequested,
        toggleSignature,
        toggleDeliveryStatus,
        toggleDispositionNotification,
        checkAndRepairFrom,
        execGetMailTipsCommand,
        messageCompose
    };
}
