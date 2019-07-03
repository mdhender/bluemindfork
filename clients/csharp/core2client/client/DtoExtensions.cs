/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using core2client;
using net.bluemind.addressbook.api;
using net.bluemind.core.api;
using net.bluemind.core.container.model.acl;
using net.bluemind.user.api;

// ReSharper disable NonReadonlyMemberInGetHashCode
// ReSharper disable InconsistentNaming

namespace core2client
{
    public static class VCardBasicAttributeExtension
    {
        public static String GetParameterValue(this VCardBasicAttribute attribute, String name)
        {
            return
                (from parameter in attribute.parameters where parameter.label == name select parameter.value)
                    .FirstOrDefault();
        }

        public static List<String> GetParameterValues(this VCardBasicAttribute attribute, String name)
        {
            var values = new List<String>(attribute.parameters.Count());
            values.AddRange(from parameter in attribute.parameters where parameter.label == name select parameter.value);
            return values;
        }
    }

    public static class UserExtension
    {
        public static Email GetDefaultEmail(this User user)
        {
            return user.emails != null ? user.emails.FirstOrDefault(email => email.isDefault) : null;
        }
    }

}

namespace net.bluemind.core.api.date
{
    public partial class BmDateTime
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(iso8601).CombineHashCode(precision).CombineHashCode(timezone);
            }
        }
    }
}

namespace net.bluemind.tag.api
{
    public partial class TagRef
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(itemUid).CombineHashCode(containerUid).CombineHashCode(label);
            }
        }
    }
}

namespace net.bluemind.calendar.api
{
    public partial class VEventChanges
    {
        public int Count
        {
            get { return add.Count + modify.Count + delete.Count; }
        }
    }

    public partial class VEventSeries
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(main)
                    .CombineHashCodeForOrderNoMatterList(occurrences);
            }
        }
    }

    public partial class VEvent
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(base.GetHashCode())
                    .CombineHashCode(dtend)
                    .CombineHashCode(transparency);
            }
        }
    }

    public partial class VEventOccurrence
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(base.GetHashCode())
                    .CombineHashCode(recurid);
            }
        }
    }
}

namespace net.bluemind.todolist.api
{
    public partial class VTodoChanges
    {
        public int Count
        {
            get { return add.Count + modify.Count + delete.Count; }
        }
    }

    public partial class VTodo
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(base.GetHashCode())
                    .CombineHashCode(completed)
                    .CombineHashCode(due)
                    .CombineHashCode(percent);
            }
        }
    }
}

namespace net.bluemind.icalendar.api
{
    public partial class ICalendarElement
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(classification)
                    .CombineHashCode(dtstart)
                    .CombineHashCodeHtml(description)
                    .CombineHashCode(location)
                    .CombineHashCode(priority)
                    //.CombineHashCode(status) FIXME
                    .CombineHashCode(summary)
                    .CombineHashCode(rrule)
                    .CombineHashCodeForOrderNoMatterList(alarm)
                    .CombineHashCodeForOrderNoMatterList(attendees)
                    .CombineHashCodeForOrderNoMatterList(categories)
                    .CombineHashCodeForOrderNoMatterList(exdate)
                    .CombineHashCodeForOrderNoMatterList(rdate);
            }
        }
    }

    public partial class ICalendarElementRRule
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(frequency).CombineHashCode(interval).CombineHashCode(until)
                    .CombineHashCodeForOrderNoMatterList(byDay)
                    .CombineHashCodeForOrderNoMatterList(byMonth);
            }
        }
    }

    public partial class ICalendarElementVAlarm
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(action).CombineHashCode(trigger);
            }
        }
    }

    public partial class ICalendarElementAttendee
    {
        [IgnoreDataMember]
        public bool IgnoreStatusInHashCode { get; set; }
        public override int GetHashCode()
        {
            unchecked
            {
                int ret = 0.CombineHashCode(mailto).CombineHashCode(role);
                if (!IgnoreStatusInHashCode)
                {
                    ret = ret.CombineHashCode(partStatus);
                }
                return ret;
            }
        }
    }
}

namespace net.bluemind.addressbook.api
{
    public partial class VCardChanges
    {
        public int Count
        {
            get { return add.Count + modify.Count + delete.Count; }
        }
    }

    #region vcard
    public partial class VCardBasicAttribute
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(value).CombineHashCodeForOrderNoMatterList(parameters);
            }
        }
    }

    public partial class VCardParameter
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(label).CombineHashCode(value);
            }
        }
    }

    public partial class VCard
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return
                    0.CombineHashCode(communications)
                        .CombineHashCodeForOrderNoMatterList(deliveryAddressing)
                        .CombineHashCode(explanatory)
                        .CombineHashCode(identification)
                        .CombineHashCode(kind)
                        .CombineHashCode(organizational)
                        .CombineHashCode(related);
            }
        }
    }

    #region VCardCommunications
    public partial class VCardCommunications
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return
                    0.CombineHashCodeForOrderNoMatterList(emails)
                        .CombineHashCodeForOrderNoMatterList(impps)
                        .CombineHashCodeForOrderNoMatterList(tels);
            }
        }
    }

    public partial class VCardCommunicationsEmail
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(base.GetHashCode());
            }
        }
    }

    public partial class VCardCommunicationsImpp
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(value);
            }
        }
    }

    public partial class VCardCommunicationsTel
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(base.GetHashCode());
            }
        }
    }
    #endregion VCardCommunications

    #region VCardDeliveryAddressing
    public partial class VCardDeliveryAddressing
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(address);
            }
        }
    }

    public partial class VCardDeliveryAddressingAddress
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return
               0.CombineHashCode(base.GetHashCode())
                   .CombineHashCode(countryName)
                   .CombineHashCode(extentedAddress)
                   .CombineHashCode(locality)
                   .CombineHashCode(postOfficeBox)
                   .CombineHashCode(postalCode)
                   .CombineHashCode(region)
                   .CombineHashCode(streetAddress);
            }
        }
    }
    #endregion VCardDeliveryAddressing

    #region VCardExplanatory
    public partial class VCardExplanatory
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return
                    0.CombineHashCodeForOrderNoMatterList(categories)
                        .CombineHashCodeHtml(note)
                        .CombineHashCodeForOrderNoMatterList(urls);
            }
        }
    }

    public partial class VCardExplanatoryUrl
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(value);
            }
        }
    }
    #endregion VCardExplanatory

    #region VCardIdentification
    public partial class VCardIdentification
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return
                    0.CombineHashCode(anniversary)
                        .CombineHashCode(birthday)
                        .CombineHashCode(formatedName)
                        .CombineHashCode(gender)
                        .CombineHashCode(name)
                        .CombineHashCode(nickname)
                        .CombineHashCode(photo);
            }
        }
    }

    public partial class VCardIdentificationFormatedName
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(base.GetHashCode());
            }
        }
    }

    public partial class VCardIdentificationGender
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(text);
            }
        }
    }

    public partial class VCardIdentificationName
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return
                    0.CombineHashCode(additionalNames)
                        .CombineHashCode(familyNames)
                        .CombineHashCode(givenNames)
                        .CombineHashCode(prefixes)
                        .CombineHashCode(suffixes);
            }
        }
    }

    public partial class VCardIdentificationNickname
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(value);
            }
        }
    }
    #endregion VCardIdentification

    #region VCardOrganizational
    public partial class VCardOrganizational
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return
                    0.CombineHashCodeForOrderNoMatterList(member)
                        .CombineHashCode(org)
                        .CombineHashCode(role)
                        .CombineHashCode(title);
            }
        }
    }

    public partial class VCardOrganizationalMember
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(containerUid).CombineHashCode(itemUid).CombineHashCode(mailto);
            }
        }
    }

    public partial class VCardOrganizationalOrg
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(company).CombineHashCode(department);
            }
        }
    }
    #endregion VCardOrganizational

    #region VCardRelated
    public partial class VCardRelated
    {
        public override int GetHashCode()
        {
            unchecked
            {
                return 0.CombineHashCode(assistant).CombineHashCode(manager).CombineHashCode(spouse);
            }
        }
    }
    #endregion VCardRelated
    #endregion card
}

namespace net.bluemind.core.container.model
{
    public partial class ContainerDescriptor
    {
        private static readonly List<Verb> _readVerbs = new List<Verb> { Verb.All, Verb.Write, Verb.Read };
        /// <summary>
        /// Extra. Has All, Write or Read verb.
        /// </summary>
        public bool Readable
        {
            get { return verbs.Intersect(_readVerbs).Any(); }
        }
    }
}