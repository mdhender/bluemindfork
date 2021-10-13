export default (userSession, calendar) => {
    return {
        value: {
            containerUid: calendar.uid,
            offlineSync: calendar.offlineSync,
            containerType: "calendar",
            owner: userSession.userId,
            defaultContainer: false,
            name: calendar.name
        },
        uid: "sub-of-" + userSession.userId + "-to-" + calendar.uid,
        internalId: null,
        version: 0,
        displayName: calendar.uid,
        externalId: null,
        createdBy: userSession.userId,
        updatedBy: userSession.userId,
        created: Date.now(),
        updated: Date.now(),
        flags: []
    };
};
