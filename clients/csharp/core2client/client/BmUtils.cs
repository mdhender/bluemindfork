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
using System.IO;
using System.Linq;
using System.Runtime.Serialization.Json;
using System.Text;
using System.Text.RegularExpressions;
using System.Xml;

namespace core2client
{
    public class BmUtils
    {
        public static String XmlToString(ref XmlDocument doc)
        {
            var builder = new StringBuilder();
            var settings = new XmlWriterSettings {Indent = false, Encoding = Encoding.UTF8};
            using (
                var writer = XmlWriter.Create((TextWriter) new StringWriterWithEncoding(builder, Encoding.UTF8),
                    settings))
            {
                doc.Save(writer);
            }
            return builder.ToString();
        }

        public static string Truncate(string source, int length)
        {
            if (source != null && source.Length > length)
            {
                source = source.Substring(0, length);
            }
            return source;
        }

        /// <summary>
        /// Return true if strIn is in valid e-mail format.
        /// Using BM regexp
        /// </summary>
        /// <param name="strIn"></param>
        /// <returns></returns>
        public static bool IsValidEmail(string strIn)
        {
            if (String.IsNullOrEmpty(strIn))
                return false;
            return Regex.IsMatch(strIn.ToLower(),
                "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@([a-z0-9-]+\\.)+[a-z]{2,}$");
        }

        public static String ObjectToJson<T>(T obj)
        {
            var dcs = new DataContractJsonSerializer(typeof (T));
            using (var ms = new MemoryStream())
            {
                dcs.WriteObject(ms, obj);
                return Encoding.UTF8.GetString(ms.ToArray());
            }
        }

        public static T JsonToObject<T>(String json)
        {
            using (var ms = new MemoryStream(Encoding.UTF8.GetBytes(json)))
            {
                var dcs = new DataContractJsonSerializer(typeof (T));
                return (T) dcs.ReadObject(ms);
            }
        }

        private const string IndentString = "  ";

        public static string FormatJson(string json)
        {

            int indentation = 0;
            int quoteCount = 0;
            var result =
                from ch in json
                let quotes = ch == '"' ? quoteCount++ : quoteCount
                let lineBreak =
                    ch == ',' && quotes%2 == 0
                        ? ch + Environment.NewLine + String.Concat(Enumerable.Repeat(IndentString, indentation))
                        : null
                let openChar =
                    ch == '{' || ch == '['
                        ? ch + Environment.NewLine + String.Concat(Enumerable.Repeat(IndentString, ++indentation))
                        : ch.ToString()
                let closeChar =
                    ch == '}' || ch == ']'
                        ? Environment.NewLine + String.Concat(Enumerable.Repeat(IndentString, --indentation)) + ch
                        : ch.ToString()
                select lineBreak ?? (openChar.Length > 1
                    ? openChar
                    : closeChar);

            return String.Concat(result);
        }
    }
}