/**
 * @fileoverview About this file
 */


/**
 * @type {Object}
 * @const
 */
var SugarCrm = {};


/**
 * Represents SugarCRM field boolean value.
 * @enum {string}
 */
SugarCrm.Boolean = {
  FALSE: '0',
  TRUE: '1'
};



/**
 * Represents SugarCRM name value list pair.
 * @constructor
 */
SugarCrm.NameValue = function() {};


/**
 * @type {string}
 */
SugarCrm.NameValue.prototype.name = '';


/**
 * @type {string}
 */
SugarCrm.NameValue.prototype.value = '';



/**
 * Represents SugarCRM name value list entry.
 * @constructor
 */
SugarCrm.NameValueEntry = function() {};


/**
 * @type {string}
 */
SugarCrm.NameValueEntry.prototype.id = '';


/**
 * @type {string}
 */
SugarCrm.NameValueEntry.prototype.module_name = '';


/**
 * @type {Object.<SugarCrm.NameValue>}
 */
SugarCrm.NameValueEntry.prototype.name_value_list = null;



/**
 * Represents SugarCRM email field.
 * @constructor
 */
SugarCrm.Bean = function() {};


/**
 * @type {string}
 */
SugarCrm.Bean.prototype.bean_id = '';


/**
 * @type {string}
 */
SugarCrm.Bean.prototype.bean_module = '';


/**
 * @type {string}
 */
SugarCrm.Bean.prototype.date_created = '';


/**
 * @type {string}
 */
SugarCrm.Bean.prototype.date_modified = '';


/**
 * @type {SugarCrm.Boolean}
 */
SugarCrm.Bean.prototype.deleted = SugarCrm.Boolean.FALSE;


/**
 * Represents SugarCRM email field.
 * @constructor
 * @extends {SugarCrm.Bean}
 */
SugarCrm.EmailField = function() {};


/**
 * @type {string}
 */
SugarCrm.EmailField.prototype.email_address = '';


/**
 * @type {string}
 */
SugarCrm.EmailField.prototype.email_address_caps = '';


/**
 * @type {string}
 */
SugarCrm.EmailField.prototype.email_address_id = '';


/**
 * @type {string}
 */
SugarCrm.Bean.prototype.id = '';


/**
 * @type {SugarCrm.Boolean}
 */
SugarCrm.EmailField.prototype.invalid_email = SugarCrm.Boolean.FALSE;


/**
 * @type {SugarCrm.Boolean}
 */
SugarCrm.EmailField.prototype.opt_out = SugarCrm.Boolean.FALSE;


/**
 * @type {SugarCrm.Boolean}
 */
SugarCrm.EmailField.prototype.primary_address = SugarCrm.Boolean.FALSE;


/**
 * @type {SugarCrm.Boolean}
 */
SugarCrm.EmailField.prototype.reply_to_address = SugarCrm.Boolean.FALSE;



/**
 * CRUD Request result.
 * @interface
 */
SugarCrm.CrudResult = function() {};


/**
 * @type {number}
 */
SugarCrm.CrudResult.prototype.created;


/**
 * @type {number}
 */
SugarCrm.CrudResult.prototype.failed;


/**
 * @type {number}
 */
SugarCrm.CrudResult.prototype.deleted;



/**
 * Id and _module.
 * @constructor
 */
SugarCrm.RecordIdentifier = function() {};


/**
 * @type {string}
 */
SugarCrm.RecordIdentifier.prototype.id = '';


/**
 * This field include in v10 REST result, but not in v4.
 * @type {string} module name.
 */
SugarCrm.RecordIdentifier.prototype._module = '';



/**
 * Id and _module.
 * @constructor
 * @extends {SugarCrm.RecordIdentifier}
 */
SugarCrm.RecordIdentifierWithName = function() {};


/**
 * @type {string}
 */
SugarCrm.RecordIdentifierWithName.prototype.name = '';



/**
 * Represents SugarCRM entry.
 * @constructor
 * @extends {SugarCrm.RecordIdentifierWithName}
 */
SugarCrm.Record = function() {};


/**
 * @type {string}
 */
SugarCrm.Record.prototype.date_entered = '';


/**
 * @type {string}
 */
SugarCrm.Record.prototype.date_modified = '';


/**
 * @type {string} either '1' or '0'
 */
SugarCrm.Record.prototype.deleted = '';


/**
 * @type {string}
 */
SugarCrm.Record.prototype.description = '';


/**
 * @type {string}
 */
SugarCrm.Record.prototype.assigned_user_id = '';


/**
 * @type {string}
 */
SugarCrm.Record.prototype.parent_id = '';


/**
 * @type {string}
 */
SugarCrm.Record.prototype.parent_name = '';


/**
 * @type {?string}
 */
SugarCrm.Record.prototype.account_id = '';


/**
 * @type {?string}
 */
SugarCrm.Record.prototype.account_name = '';


/**
 * @type {string}
 */
SugarCrm.Record.prototype.parent_type = '';



/**
 * Result record from query with scoring.
 * @constructor
 * @extends {SugarCrm.Record}
 */
SugarCrm.ScoredRecord = function() {};


/**
 * @type {number}
 */
SugarCrm.ScoredRecord.prototype._score = 0;



/**
 * OAuth2 respond.
 * @constructor
 */
SugarCrm.OauthToken = function() {};


/**
 * @type {string}
 */
SugarCrm.LoginRecord.prototype.access_token = '';


/**
 * @type {number}
 */
SugarCrm.LoginRecord.prototype.expires_in = 0;


/**
 * @type {number} computed value.
 */
SugarCrm.LoginRecord.prototype.expires_at = 0;


/**
 * @type {string}
 */
SugarCrm.LoginRecord.prototype.token_type = '';


/**
 * @type {?string}
 */
SugarCrm.LoginRecord.prototype.scope = null;


/**
 * @type {string}
 */
SugarCrm.LoginRecord.prototype.refresh_token = '';


/**
 * @type {string}
 */
SugarCrm.LoginRecord.prototype.download_token = '';


/**
 * @type {number}
 */
SugarCrm.LoginRecord.prototype.refresh_expires_in = 0;


/**
 * @type {number} computed value
 */
SugarCrm.LoginRecord.prototype.refresh_expires_at = 0;



/**
 * Record in login respond.
 * @constructor
 * @extends {SugarCrm.Record}
 */
SugarCrm.LoginRecord = function() {};


/**
 * @type {string} "-99"
 */
SugarCrm.LoginRecord.prototype.user_currency_id = '';


/**
 * @type {string} "US Dollars"
 */
SugarCrm.LoginRecord.prototype.user_currency_name = '';


/**
 * @type {string} "."
 */
SugarCrm.LoginRecord.prototype.user_decimal_seperator = '';


/**
 * @type {string} "m/d/Y"
 */
SugarCrm.LoginRecord.prototype.user_default_dateformat = '';


/**
 * @type {?string}
 */
SugarCrm.LoginRecord.prototype.user_default_team_id = '';


/**
 * @type {string} "h:ia"
 */
SugarCrm.LoginRecord.prototype.user_default_timeformat = '';


/**
 * @type {string}
 */
SugarCrm.LoginRecord.prototype.user_id = '';


/**
 * @type {boolean}
 */
SugarCrm.LoginRecord.prototype.user_is_admin = false;



/**
 * Represents SugarCRM ModuleField.
 * @constructor
 */
SugarCrm.ModuleField = function() {};


/**
 * @type {boolean}
 */
SugarCrm.ModuleField.prototype.calculated = false;


/**
 * @type {string}
 */
SugarCrm.ModuleField.prototype.group = '';


/**
 * @type {string}
 */
SugarCrm.ModuleField.prototype.id_name = '';


/**
 * @type {string}
 */
SugarCrm.ModuleField.prototype.label = '';


/**
 * @type {string}
 */
SugarCrm.ModuleField.prototype.len = '';


/**
 * @type {string}
 */
SugarCrm.ModuleField.prototype.name = '';


/**
 * @type {Object.<SugarCrm.NameValue>|undefined}
 */
SugarCrm.ModuleField.prototype.options = {};


/**
 * @type {string}
 */
SugarCrm.ModuleField.prototype.related_module = '';


/**
 * @type {number}
 */
SugarCrm.ModuleField.prototype.required = 0;


/**
 * @type {string}
 */
SugarCrm.ModuleField.prototype.type = '';



/**
 * Represents SugarCRM LinkField.
 * @constructor
 */
SugarCrm.LinkField = function() {};


/**
 * @type {string} this is always 'link' ?
 */
SugarCrm.LinkField.prototype.type = '';


/**
 * @type {string} Module name
 */
SugarCrm.LinkField.prototype.module = '';


/**
 * @type {string} Link name. Same as its object hash key.
 */
SugarCrm.LinkField.prototype.name = '';



/**
 * Represents meta data for ModuleInfo.
 * @constructor
 */
SugarCrm.ModuleMeta = function() {};


/**
 * Color hex for module.
 * @type {string}
 */
SugarCrm.ModuleMeta.prototype.color = '';


/**
 * List of email fields.
 * @type {Array.<string>}
 */
SugarCrm.ModuleMeta.prototype.email_fields = [];



/**
 * Represents SugarCRM Acl.
 * @constructor
 */
SugarCrm.Acl = function() {};


/**
 * @type {boolean}
 */
SugarCrm.Acl.prototype.access = false;


/**
 * Enum of 'edit', 'delete', 'list', 'view', 'import', 'export'
 * @type {string}
 */
SugarCrm.Acl.prototype.action = '';



/**
 * Represents SugarCRM AvailableModule.
 * @constructor
 */
SugarCrm.AvailableModule = function() {};


/**
 * @type {boolean}
 */
SugarCrm.AvailableModule.prototype.favorite_enabled = false;


/**
 * @type {string}
 */
SugarCrm.AvailableModule.prototype.module_key = '';


/**
 * @type {string}
 */
SugarCrm.AvailableModule.prototype.module_label = '';


/**
 * @type {Array.<SugarCrm.Acl>}
 */
SugarCrm.AvailableModule.prototype.acls = [];



/**
 * Represents SugarCRM ModuleInfo.
 * @constructor
 */
SugarCrm.ModuleInfo = function() {};


/**
 * @type {SugarCrm.ModuleMeta}
 */
SugarCrm.ModuleInfo.prototype._meta = null;


/**
 * @type {string}
 */
SugarCrm.ModuleInfo.prototype.module_name = '';


/**
 * @type {string}
 */
SugarCrm.ModuleInfo.prototype.table_name = '';


/**
 * @type {!Object.<SugarCrm.ModuleField>|!Array.<SugarCrm.ModuleField>}
 */
SugarCrm.ModuleInfo.prototype.module_fields = [];


/**
 * @type {!Object.<SugarCrm.LinkField>}
 */
SugarCrm.ModuleInfo.prototype.link_fields = {};



/**
 * Represents query format in channel.
 * @constructor
 * @extends {CrmApp.ReqQuery}
 */
SugarCrm.Query = function() {};


/**
 * @type {Array.<SugarCrm.Record>} result of query.
 */
SugarCrm.Query.prototype.result = [];



/**
 * @constructor
 */
SugarCrm.ServerInfo = function() {};


/**
 * @type {(string|undefined)}
 */
SugarCrm.ServerInfo.prototype.flavor = '';


/**
 * @type {(string|undefined)}
 */
SugarCrm.ServerInfo.prototype.version = '';


/**
 * @type {(string|undefined)}
 */
SugarCrm.ServerInfo.prototype.uptime = '';



/**
 * @constructor
 */
SugarCrm.About = function() {};


/**
 * @type {(string|undefined)}
 */
SugarCrm.About.prototype.baseUrl = '';


/**
 * @type {string}
 */
SugarCrm.About.prototype.domain = '';


/**
 * @type {(string|undefined)}
 */
SugarCrm.About.prototype.userName = '';


/**
 * @type {(boolean|undefined)}
 */
SugarCrm.About.prototype.hostPermission = false;


/**
 * @type {(string|undefined)} authentication provider.
 */
SugarCrm.About.prototype.provider = '';


/**
 * @type {(boolean|undefined)}
 */
SugarCrm.About.prototype.isLogin = false;


/**
 * @type {YdnCrm.UserSiteLicense}
 */
SugarCrm.About.prototype.siteLicense = null;



/**
 * @constructor
 * @extends {SugarCrm.About}
 */
CrmApp.Credential = function() {};


/**
 * @type {string}
 */
CrmApp.Credential.prototype.password = '';


/**
 * @type {string|undefined}
 */
CrmApp.Credential.prototype.provider = '';


/**
 * @type {boolean|undefined}
 */
CrmApp.Credential.prototype.hashed = false;



/**
 * @constructor
 */
SugarCrm.Details = function() {};


/**
 * @type {SugarCrm.About}
 */
SugarCrm.Details.prototype.about = null;


/**
 * @type {SugarCrm.ServerInfo}
 */
SugarCrm.Details.prototype.serverInfo = null;


/**
 * @type {?SugarCrm.LoginRecord}
 */
SugarCrm.Details.prototype.loginInfo = null;


/**
 * @type {?CrmApp.Credential}
 */
SugarCrm.Details.prototype.credential = null;


/**
 * @type {Array<SugarCrm.AvailableModule>}
 */
SugarCrm.Details.prototype.availableModules = [];


/**
 * @type {Array.<SugarCrm.ModuleInfo>}
 */
SugarCrm.Details.prototype.modulesInfo = [];



/**
 * Query result for GData contact to SugarCRM records.
 * @interface
 */
SugarCrm.ContactSimilarityResult = function() {};


/**
 * @type {string}
 */
SugarCrm.ContactSimilarityResult.prototype.module;


/**
 * @type {string}
 */
SugarCrm.ContactSimilarityResult.prototype.index;


/**
 * @type {string}
 */
SugarCrm.ContactSimilarityResult.prototype.key;


/**
 * @type {Array<SugarCrm.Record>}
 */
SugarCrm.ContactSimilarityResult.prototype.result;



/**
 * Module name and id pair in relationship result.
 * @constructor
 */
SugarCrm.ModuleNameIdPair = function() {};


/**
 * @type {string}
 */
SugarCrm.ModuleNameIdPair.prototype.module_name = '';


/**
 * @type {string}
 */
SugarCrm.ModuleNameIdPair.prototype.id = '';



/**
 * Module name and id pair in relationship result.
 * @constructor
 * @extends {SugarCrm.ModuleNameIdPair}
 */
SugarCrm.ModuleNameIdPairWithLabel = function() {};


/**
 * @type {string}
 */
SugarCrm.ModuleNameIdPairWithLabel.prototype.label = '';



/**
 * Record format for relationships cache.
 * @constructor
 */
SugarCrm.Relationships = function() {};


/**
 * @type {number}
 */
SugarCrm.Relationships.prototype.modified;


/**
 * @type {string}
 */
SugarCrm.Relationships.prototype.date_modified;


/**
 * @type {SugarCrm.ModuleNameIdPair}
 */
SugarCrm.Relationships.prototype.to;


/**
 * @type {SugarCrm.ModuleNameIdPair}
 */
SugarCrm.Relationships.prototype.from;



/**
 * Represents SugarCRM entry.
 * @constructor
 */
SugarCrm.SyncCheckpoint = function() {};


/**
 * Lower value of updated timestamp.
 * @type {string}
 */
SugarCrm.SyncCheckpoint.prototype.lower = '';


/**
 * Lower record id.
 * @type {string}
 */
SugarCrm.SyncCheckpoint.prototype.lowerId = '';


/**
 * Upper value of updated timestamp.
 * @type {string}
 */
SugarCrm.SyncCheckpoint.prototype.upper = '';


/**
 * Upper record id.
 * @type {string}
 */
SugarCrm.SyncCheckpoint.prototype.upperId = '';


/**
 * Updated timestamp.
 * @type {number}
 */
SugarCrm.SyncCheckpoint.prototype.updated = 0;
