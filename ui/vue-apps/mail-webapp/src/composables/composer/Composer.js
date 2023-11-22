import { ERROR, REMOVE } from "@bluemind/alert.store";
import { draftUtils, messageUtils } from "@bluemind/mail";
import { DEBOUNCED_SAVE_MESSAGE, REQUEST_DSN, TOGGLE_DSN_REQUEST } from "~/actions";
import { RESET_COMPOSER, SET_MESSAGE_HEADERS, SET_SAVE_ERROR } from "~/mutations";
import { useGetMailTipsCommand } from "~/commands";
import { IS_SENDER_SHOWN } from "~/getters";
import { Flag } from "@bluemind/email";
import store from "@bluemind/store";
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { setFrom, getIdentityId } from "./ComposerFrom";

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
    const identityId = computed(() => getIdentityId(message.value.headers));
    const identities = computed(() => store.state["root-app"].identities);
    const defaultIdentity = computed(() => store.getters["root-app/DEFAULT_IDENTITY"]);

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

    onMounted(async () => {
        store.commit("mail/" + SET_SAVE_ERROR, {});
        if (message.value.from) {
            const draftIdentityId = getIdentityId(message.value.headers);
            await checkAndRepairFrom(draftIdentityId);
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
        store.dispatch(`mail/${DEBOUNCED_SAVE_MESSAGE}`, { draft: message.value });
    }

    function toggleDispositionNotification() {
        const headers = [...message.value.headers];
        if (isDispositionNotificationRequested.value) {
            messageUtils.removeDispositionNotificationHeader(headers);
        } else {
            messageUtils.setDispositionNotificationHeader(headers, message.value.from);
        }
        store.commit(`mail/${SET_MESSAGE_HEADERS}`, { messageKey: message.value.key, headers });
        store.dispatch(`mail/${DEBOUNCED_SAVE_MESSAGE}`, { draft: message.value });
    }

    async function checkAndRepairFrom(identityId) {
        const identity = resolveIdentity(identityId);
        setFrom(identity, message.value);
    }

    function resolveIdentity(identityId) {
        console.log(
            findIdentityById(identityId) || findIdentityByOriginator(message.value.from) || defaultIdentity.value
        );
        return findIdentityById(identityId) || findIdentityByOriginator(message.value.from) || defaultIdentity.value;
    }

    function findIdentityById(id) {
        return identities.value.find(i => i.id === id);
    }

    function findIdentityByOriginator(originator) {
        if (defaultIdentity.value.email === originator.address && defaultIdentity.value.displayname && originator.dn) {
            return defaultIdentity.value;
        } else {
            return identities.value.find(
                identity => identity.email === originator.address && identity.displayname && originator.dn
            );
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
        identityId,
        isSenderShown,
        isDeliveryStatusRequested,
        isDispositionNotificationRequested,
        checkAndRepairFrom,
        toggleSignature,
        toggleDeliveryStatus,
        toggleDispositionNotification,
        execGetMailTipsCommand,
        messageCompose
    };
}
