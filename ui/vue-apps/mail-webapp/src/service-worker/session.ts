import { MailAPI } from "./MailAPI";
import { MailDB } from "./MailDB";
import { SessionInfo } from "./entry";

let instance: Session | null = null;

export default class Session {
    infos: SessionInfo;
    _api: MailAPI | null;
    _db: MailDB | null;

    constructor(infos: SessionInfo) {
        this.infos = infos;
        this._api = null;
        this._db = null;
    }

    get api(): MailAPI {
        if (!this._api) {
            const { sid } = this.infos;
            this._api = new MailAPI({ sid });
        }
        return this._api;
    }

    get db(): MailDB {
        if (!this._db) {
            const dbName = this.userAtDomain;
            this._db = new MailDB(dbName);
        }
        return this._db;
    }

    get userAtDomain(): string {
        const { userId, domain } = this.infos;
        return `user.${userId}@${domain.replace(".", "_")}`;
    }

    static async instance(): Promise<Session> {
        if (!instance) {
            const infos = await MailAPI.fetchSessionInfos();
            instance = new Session(infos);
        }
        return instance;
    }

    static async infos(): Promise<SessionInfo> {
        return (await Session.instance()).infos;
    }

    static async api(): Promise<MailAPI> {
        return (await Session.instance()).api;
    }

    static async db(): Promise<MailDB> {
        return (await Session.instance()).db;
    }

    static async userAtDomain() {
        return (await Session.instance()).userAtDomain;
    }

    static clear() {
        instance = null;
    }
}
