export const ContainerType = {
    CALENDAR: "calendar",
    MAILBOX: "mailboxacl"
};

export function containerToSubscription(userSession, { uid, name, offlineSync }) {
    return {
        value: {
            containerUid: uid,
            offlineSync: offlineSync,
            containerType: "calendar",
            owner: userSession.userId,
            defaultContainer: false,
            name: name
        },
        uid: "sub-of-" + userSession.userId + "-to-" + uid,
        internalId: null,
        version: 0,
        displayName: uid,
        externalId: null,
        createdBy: userSession.userId,
        updatedBy: userSession.userId,
        created: Date.now(),
        updated: Date.now(),
        flags: []
    };
}
