import global from "@bluemind/global";

export default global.$imapLock || (global.$imapLock = Promise.resolve());
