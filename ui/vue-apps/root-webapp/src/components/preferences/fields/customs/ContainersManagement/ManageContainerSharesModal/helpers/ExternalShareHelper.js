import { PublishMode } from "@bluemind/calendar.api";
import { inject } from "@bluemind/inject";
import UUIDHelper from "@bluemind/uuid";

export async function loadCalendarUrls(calendarUid) {
    let publicUrls = await inject("PublishCalendarPersistence", calendarUid).getGeneratedUrls(PublishMode.PUBLIC);
    let privateUrls = await inject("PublishCalendarPersistence", calendarUid).getGeneratedUrls(PublishMode.PRIVATE);
    publicUrls = publicUrls.map(url => ({ url, publishMode: PublishMode.PUBLIC }));
    privateUrls = privateUrls.map(url => ({ url, publishMode: PublishMode.PRIVATE }));

    let externalShares = publicUrls.concat(privateUrls).map(share => {
        const lastUrlPart = share.url.substring(share.url.lastIndexOf("/") + 1, share.url.length);
        const token = lastUrlPart.replace("x-calendar-" + share.publishMode.toLowerCase() + "-", "");
        return { ...share, token };
    });
    const uids = externalShares.filter(share => UUIDHelper.isUUID(share.token)).map(share => share.token);
    if (uids.length > 0) {
        const query = uids.map(uid => "uid:" + uid).join(" OR ");
        const vcards = await inject("AddressBooksPersistence").search({ query });
        vcards.values.forEach(vcard => {
            const share = externalShares.find(share => share.token === vcard.uid);
            share.vcard = vcard;
        });
    }

    return externalShares;
}

export function sendExternalToServer(publishMode, shareToken, calendarUid) {
    return shareToken && UUIDHelper.isUUID(shareToken)
        ? inject("PublishCalendarPersistence", calendarUid).createUrl(publishMode, shareToken)
        : inject("PublishCalendarPersistence", calendarUid).generateUrl(publishMode);
}

export function urlToAclSubject({ url }) {
    return url.substring(url.lastIndexOf("/") + 1, url.length);
}

export function publishModeOptions(i18n) {
    return [
        {
            text: i18n.t("preferences.calendar.my_calendars.publish_link_public_mode"),
            value: PublishMode.PUBLIC
        },
        {
            text: i18n.t("preferences.calendar.my_calendars.publish_link_private_mode"),
            value: PublishMode.PRIVATE
        }
    ];
}
