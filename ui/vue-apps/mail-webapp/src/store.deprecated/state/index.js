import { STATUS } from "../constants";

export let userUid;
export let maxMessageSize;
export let messageFilter;
export let messagesWithUnblockedRemoteImages = [];
export let selectedMessageKeys = [];
export let showBlockedImagesAlert = false;
export const status = STATUS.IDLE;
