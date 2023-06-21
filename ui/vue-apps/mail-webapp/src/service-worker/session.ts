import { Session } from "@bluemind/session";
import { MailAPI } from "./MailAPI";

let instance: SessionWrapper | null = null;

export default class SessionWrapper {
    infos: Session;

    constructor(infos: Session) {
        this.infos = infos;
    }

    get userAtDomain(): string {
        const { userId, domain } = this.infos;
        return `user.${userId}@${domain.replace(/\./g, "_")}`;
    }

    static async instance(): Promise<SessionWrapper> {
        if (!instance) {
            const infos = await MailAPI.fetchSessionInfos();
            instance = new SessionWrapper(infos);
        }
        return instance;
    }

    static async infos(): Promise<Session> {
        return (await SessionWrapper.instance()).infos;
    }

    static async userAtDomain() {
        return (await SessionWrapper.instance()).userAtDomain;
    }

    static clear() {
        instance = null;
    }
}
