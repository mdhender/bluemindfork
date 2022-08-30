export function getMetadatas() {
    return {
        doc: " Container of {@link MailboxItem}.\n \n The container is created by the {@link IMailboxFolders} service when a new\n replicated folder is created.\n",
        className: "IMailboxItems",
        packageName: "net.bluemind.backend.mail.api",
        path: {
            value: "/mail_items/{replicatedMailboxUid}",
            parameters: ["replicatedMailboxUid"]
        },
        methods: [
            {
                path: {
                    value: "_filteredChangesetById",
                    parameters: []
                },
                verb: "POST",
                name: "filteredChangesetById",
                beta: false,
                outParam: {
                    name: "net.bluemind.core.container.model.ContainerChangeset",
                    parameters: [
                        {
                            name: "net.bluemind.core.container.model.ItemVersion"
                        }
                    ]
                },
                inParams: [
                    {
                        type: {
                            name: "Long"
                        },
                        name: "since",
                        paramType: "QueryParam"
                    },
                    {
                        type: {
                            name: "net.bluemind.core.container.model.ItemFlagFilter"
                        },
                        name: "arg1",
                        paramType: "Body"
                    }
                ]
            },
            {
                path: {
                    value: "{id}/getForUpdate",
                    parameters: ["id"]
                },
                verb: "GET",
                name: "getForUpdate",
                beta: false,
                outParam: {
                    name: "net.bluemind.core.container.model.ItemValue",
                    parameters: [
                        {
                            name: "net.bluemind.backend.mail.api.MailboxItem"
                        }
                    ]
                },
                inParams: [
                    {
                        type: {
                            name: "long"
                        },
                        name: "id",
                        paramType: "PathParam"
                    }
                ],
                doc: " Decompose EML in temporary parts, useful to update drafts\n \n",
                outDoc: "message structure with temporary addresses\n"
            },
            {
                path: {
                    value: "_changeset",
                    parameters: []
                },
                verb: "GET",
                name: "changeset",
                beta: false,
                outParam: {
                    name: "net.bluemind.core.container.model.ContainerChangeset",
                    parameters: [
                        {
                            name: "String"
                        }
                    ]
                },
                inParams: [
                    {
                        type: {
                            name: "Long"
                        },
                        name: "since",
                        paramType: "QueryParam"
                    }
                ]
            },
            {
                path: {
                    value: "_deleteFlag",
                    parameters: []
                },
                verb: "POST",
                name: "deleteFlag",
                beta: false,
                outParam: {
                    name: "net.bluemind.core.container.api.Ack"
                },
                inParams: [
                    {
                        type: {
                            name: "net.bluemind.backend.mail.api.flags.FlagUpdate"
                        },
                        name: "flagUpdate",
                        paramType: "Body"
                    }
                ],
                doc: " Delete one flag to multiple {@link MailboxItem}.\n \n",
                outDoc: "the new container version\n"
            },
            {
                path: {
                    value: "_changesetById",
                    parameters: []
                },
                verb: "GET",
                name: "changesetById",
                beta: false,
                outParam: {
                    name: "net.bluemind.core.container.model.ContainerChangeset",
                    parameters: [
                        {
                            name: "Long"
                        }
                    ]
                },
                inParams: [
                    {
                        type: {
                            name: "Long"
                        },
                        name: "since",
                        paramType: "QueryParam"
                    }
                ]
            },
            {
                path: {
                    value: "_unread",
                    parameters: []
                },
                verb: "GET",
                name: "unreadItems",
                beta: false,
                outParam: {
                    name: "List",
                    parameters: [
                        {
                            name: "Long"
                        }
                    ]
                },
                doc: " Get the list of unread items, applying the per-user overlay when dealing with\n a shared folder.\n \n",
                outDoc: "the list of {@link ItemValue#internalId}\n"
            },
            {
                path: {
                    value: "_part",
                    parameters: []
                },
                verb: "PUT",
                name: "uploadPart",
                beta: false,
                outParam: {
                    name: "String"
                },
                inParams: [
                    {
                        type: {
                            name: "Stream"
                        },
                        name: "part",
                        paramType: "Body",
                        doc: "a re-usable email part.\n"
                    }
                ],
                doc: " Upload an email part (eg. attachment, html body). The returned address can be\n used as {@link Part#address} when creating or updating a {@link MailboxItem}.\n \n The uploaded parts need to be cleaned-up explicitly with\n {@link IMailboxItems#removePart(String)}\n \n",
                outDoc: "an address usable as {@link Part#address}\n"
            },
            {
                path: {
                    value: "",
                    parameters: []
                },
                verb: "PUT",
                name: "create",
                beta: false,
                outParam: {
                    name: "net.bluemind.backend.mail.api.ImapItemIdentifier"
                },
                inParams: [
                    {
                        type: {
                            name: "net.bluemind.backend.mail.api.MailboxItem"
                        },
                        name: "value",
                        paramType: "Body"
                    }
                ]
            },
            {
                path: {
                    value: "{id}/completeById",
                    parameters: ["id"]
                },
                verb: "GET",
                name: "getCompleteById",
                beta: false,
                outParam: {
                    name: "net.bluemind.core.container.model.ItemValue",
                    parameters: [
                        {
                            name: "net.bluemind.backend.mail.api.MailboxItem"
                        }
                    ]
                },
                inParams: [
                    {
                        type: {
                            name: "long"
                        },
                        name: "id",
                        paramType: "PathParam"
                    }
                ]
            },
            {
                path: {
                    value: "_recent",
                    parameters: []
                },
                verb: "GET",
                name: "recentItems",
                beta: false,
                outParam: {
                    name: "List",
                    parameters: [
                        {
                            name: "Long"
                        }
                    ]
                },
                inParams: [
                    {
                        type: {
                            name: "java.util.Date"
                        },
                        name: "deliveredOrUpdatedAfter",
                        paramType: "Body"
                    }
                ],
                doc: " Get the list of {@link ItemValue#internalId} for {@link MailboxItem}\n delivered or updated after or at the given date.\n \n",
                outDoc: "\n"
            },
            {
                path: {
                    value: "_unexpunge/{itemId}",
                    parameters: ["itemId"]
                },
                verb: "POST",
                name: "unexpunge",
                beta: false,
                outParam: {
                    name: "net.bluemind.core.container.model.ItemIdentifier"
                },
                inParams: [
                    {
                        type: {
                            name: "long"
                        },
                        name: "itemId",
                        paramType: "PathParam",
                        doc: "the item id of a deleted or deleted+expunged message\n"
                    }
                ],
                doc: " Re-injects a deleted item into the current folder\n \n",
                outDoc: "\n"
            },
            {
                path: {
                    value: "_version",
                    parameters: []
                },
                verb: "GET",
                name: "getVersion",
                beta: false,
                outParam: {
                    name: "long"
                }
            },
            {
                path: {
                    value: "_expunge",
                    parameters: []
                },
                verb: "POST",
                name: "expunge",
                beta: false,
                outParam: {
                    name: "void"
                },
                doc: " Mark deleted items as ready for removal. Physical will removal will occur\n later (cyr_expire & co)\n"
            },
            {
                path: {
                    value: "_count",
                    parameters: []
                },
                verb: "POST",
                name: "count",
                beta: false,
                outParam: {
                    name: "net.bluemind.core.container.api.Count"
                },
                inParams: [
                    {
                        type: {
                            name: "net.bluemind.core.container.model.ItemFlagFilter"
                        },
                        name: "arg0",
                        paramType: "Body"
                    }
                ]
            },
            {
                path: {
                    value: "eml/{imapUid}",
                    parameters: ["imapUid"]
                },
                verb: "GET",
                name: "fetchComplete",
                beta: false,
                outParam: {
                    name: "Stream"
                },
                inParams: [
                    {
                        type: {
                            name: "long"
                        },
                        name: "imapUid",
                        paramType: "PathParam"
                    }
                ],
                doc: "",
                outDoc: "\n"
            },
            {
                path: {
                    value: "_sorted",
                    parameters: []
                },
                verb: "POST",
                name: "sortedIds",
                beta: false,
                outParam: {
                    name: "List",
                    parameters: [
                        {
                            name: "Long"
                        }
                    ]
                },
                inParams: [
                    {
                        type: {
                            name: "net.bluemind.core.container.model.SortDescriptor"
                        },
                        name: "sorted",
                        paramType: "Body"
                    }
                ]
            },
            {
                path: {
                    value: "{uid}/_itemchangelog",
                    parameters: ["uid"]
                },
                verb: "GET",
                name: "itemChangelog",
                beta: false,
                outParam: {
                    name: "net.bluemind.core.container.model.ItemChangelog"
                },
                inParams: [
                    {
                        type: {
                            name: "String"
                        },
                        name: "uid",
                        paramType: "PathParam"
                    },
                    {
                        type: {
                            name: "Long"
                        },
                        name: "arg1",
                        paramType: "Body"
                    }
                ]
            },
            {
                path: {
                    value: "_addFlag",
                    parameters: []
                },
                verb: "PUT",
                name: "addFlag",
                beta: false,
                outParam: {
                    name: "net.bluemind.core.container.api.Ack"
                },
                inParams: [
                    {
                        type: {
                            name: "net.bluemind.backend.mail.api.flags.FlagUpdate"
                        },
                        name: "flagUpdate",
                        paramType: "Body"
                    }
                ],
                doc: " Add one flag to multiple {@link MailboxItem}.\n \n",
                outDoc: "the new container version\n"
            },
            {
                path: {
                    value: "_multipleDelete",
                    parameters: []
                },
                verb: "DELETE",
                name: "multipleDeleteById",
                beta: false,
                outParam: {
                    name: "void"
                },
                inParams: [
                    {
                        type: {
                            name: "List",
                            parameters: [
                                {
                                    name: "Long"
                                }
                            ]
                        },
                        name: "arg0",
                        paramType: "Body"
                    }
                ]
            },
            {
                path: {
                    value: "{partId}/_part",
                    parameters: ["partId"]
                },
                verb: "DELETE",
                name: "removePart",
                beta: false,
                outParam: {
                    name: "void"
                },
                inParams: [
                    {
                        type: {
                            name: "String"
                        },
                        name: "partId",
                        paramType: "PathParam",
                        doc: "an address returned by a previous <code>uploadPart</code> call\n"
                    }
                ],
                doc: " Remove a part uploaded through {@link IMailboxItems#uploadPart(Stream)}\n \n"
            },
            {
                path: {
                    value: "id/{id}",
                    parameters: ["id"]
                },
                verb: "POST",
                name: "updateById",
                beta: false,
                outParam: {
                    name: "net.bluemind.backend.mail.api.ImapAck"
                },
                inParams: [
                    {
                        type: {
                            name: "long"
                        },
                        name: "id",
                        paramType: "PathParam"
                    },
                    {
                        type: {
                            name: "net.bluemind.backend.mail.api.MailboxItem"
                        },
                        name: "value",
                        paramType: "Body"
                    }
                ]
            },
            {
                path: {
                    value: "_mgetById",
                    parameters: []
                },
                verb: "POST",
                name: "multipleGetById",
                beta: false,
                outParam: {
                    name: "List",
                    parameters: [
                        {
                            name: "net.bluemind.core.container.model.ItemValue",
                            parameters: [
                                {
                                    name: "net.bluemind.backend.mail.api.MailboxItem"
                                }
                            ]
                        }
                    ]
                },
                inParams: [
                    {
                        type: {
                            name: "List",
                            parameters: [
                                {
                                    name: "Long"
                                }
                            ]
                        },
                        name: "ids",
                        paramType: "Body"
                    }
                ]
            },
            {
                path: {
                    value: "_multipleById",
                    parameters: []
                },
                verb: "POST",
                name: "multipleById",
                beta: false,
                outParam: {
                    name: "List",
                    parameters: [
                        {
                            name: "net.bluemind.core.container.model.ItemValue",
                            parameters: [
                                {
                                    name: "net.bluemind.backend.mail.api.MailboxItem"
                                }
                            ]
                        }
                    ]
                },
                inParams: [
                    {
                        type: {
                            name: "List",
                            parameters: [
                                {
                                    name: "Long"
                                }
                            ]
                        },
                        name: "ids",
                        paramType: "Body"
                    }
                ],
                doc: " \n @Deprecated prefer {@link IMailboxItems#multipleGetById(List)} as it is\n             defined in {@link ICrudByIdSupport}\n \n",
                outDoc: "\n"
            },
            {
                path: {
                    value: "_changelog",
                    parameters: []
                },
                verb: "GET",
                name: "containerChangelog",
                beta: false,
                outParam: {
                    name: "net.bluemind.core.container.model.ContainerChangelog"
                },
                inParams: [
                    {
                        type: {
                            name: "Long"
                        },
                        name: "arg0",
                        paramType: "Body"
                    }
                ]
            },
            {
                path: {
                    value: "part/{imapUid}/{address}",
                    parameters: ["imapUid", "address"]
                },
                verb: "GET",
                name: "fetch",
                beta: false,
                outParam: {
                    name: "Stream"
                },
                inParams: [
                    {
                        type: {
                            name: "long"
                        },
                        name: "imapUid",
                        paramType: "PathParam"
                    },
                    {
                        type: {
                            name: "String"
                        },
                        name: "address",
                        paramType: "PathParam"
                    },
                    {
                        type: {
                            name: "String"
                        },
                        name: "encoding",
                        paramType: "QueryParam",
                        doc: "set null to fetch pristine part\n"
                    },
                    {
                        type: {
                            name: "String"
                        },
                        name: "mime",
                        paramType: "QueryParam",
                        doc: "    override the mime type of the response\n"
                    },
                    {
                        type: {
                            name: "String"
                        },
                        name: "charset",
                        paramType: "QueryParam",
                        doc: " override the charset of the response\n"
                    },
                    {
                        type: {
                            name: "String"
                        },
                        name: "filename",
                        paramType: "QueryParam",
                        doc: "set a part name (useful for download purpose)\n"
                    }
                ],
                doc: " Fetch a single part from an email mime tree. The address, encoding & charset\n are specified in the {@link Part} objects from {@link MessageBody#structure}.\n \n",
                outDoc: "a stream of the (optionally) decoded part\n"
            },
            {
                path: {
                    value: "id/{id}",
                    parameters: ["id"]
                },
                verb: "DELETE",
                name: "deleteById",
                beta: false,
                outParam: {
                    name: "void"
                },
                inParams: [
                    {
                        type: {
                            name: "long"
                        },
                        name: "id",
                        paramType: "PathParam"
                    }
                ]
            },
            {
                path: {
                    value: "id/{id}",
                    parameters: ["id"]
                },
                verb: "PUT",
                name: "createById",
                beta: false,
                outParam: {
                    name: "net.bluemind.backend.mail.api.ImapAck"
                },
                inParams: [
                    {
                        type: {
                            name: "long"
                        },
                        name: "id",
                        paramType: "PathParam"
                    },
                    {
                        type: {
                            name: "net.bluemind.backend.mail.api.MailboxItem"
                        },
                        name: "value",
                        paramType: "Body"
                    }
                ]
            },
            {
                path: {
                    value: "_itemIds",
                    parameters: []
                },
                verb: "GET",
                name: "allIds",
                beta: false,
                outParam: {
                    name: "net.bluemind.core.api.ListResult",
                    parameters: [
                        {
                            name: "Long"
                        }
                    ]
                },
                inParams: [
                    {
                        type: {
                            name: "String"
                        },
                        name: "filter",
                        paramType: "QueryParam"
                    },
                    {
                        type: {
                            name: "Long"
                        },
                        name: "knownContainerVersion",
                        paramType: "QueryParam"
                    },
                    {
                        type: {
                            name: "Integer"
                        },
                        name: "limit",
                        paramType: "QueryParam"
                    },
                    {
                        type: {
                            name: "Integer"
                        },
                        name: "offset",
                        paramType: "QueryParam"
                    }
                ]
            }
        ],
        internal: false
    };
}
