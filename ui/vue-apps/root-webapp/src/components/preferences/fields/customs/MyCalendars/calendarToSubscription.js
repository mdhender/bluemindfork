export default (userSession, calendarUid, calendarName) => {
    return {
        value: {
            containerUid: calendarUid,
            offlineSync: true,
            containerType: "calendar",
            owner: userSession.userId,
            defaultContainer: false,
            name: calendarName
        },
        uid: "sub-of-" + userSession.userId + "-to-" + calendarUid,
        internalId: null,
        version: 0,
        displayName: calendarUid,
        externalId: null,
        createdBy: userSession.userId,
        updatedBy: userSession.userId,
        created: Date.now(),
        updated: Date.now(),
        flags: []
    };
};
