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
using System.Diagnostics;
using System.Linq;
using System.Text.RegularExpressions;
using HtmlAgilityPack;

namespace core2client
{
    public static class HashHelper
    {
        public static int GetHashCode<T>(T[] list)
        {
            unchecked
            {
                int ret = list.Aggregate(0, (current, item) => (current*397) ^ (item != null ? item.GetHashCode() : 0));
#if DEBUG
                Debug.WriteLine("" + ret + " <- GetHashCode[<" + typeof(T) + ">](" + list + ")");
#endif
                return ret;
            }
        }

        public static int GetHashCode<T>(IEnumerable<T> list)
        {
            unchecked
            {
                int ret = list.Aggregate(0, (current, item) => (current*397) ^ (item != null ? item.GetHashCode() : 0));
#if DEBUG
                Debug.WriteLine("" + ret + " <- GetHashCodeList<List<" + typeof(T) + ">>(" + list + ")");
#endif
                return ret;
            }
        }

        /// <summary>
        /// Gets a hashcode for a List for that the order of items does not matter.
        /// So {1, 2, 3} and {3, 2, 1} will get same hash code.
        /// </summary>
        public static int CombineHashCodeForOrderNoMatterList<T>(this int hashCode, List<T> arg)
        {
            unchecked
            {
                if (arg == null || arg.Count == 0) return (hashCode*397) ^ 0;
                int hash = 0;
                int count = 0;
                foreach (var item in arg)
                {
                    hash += item.GetHashCode();
                    count++;
                }
                int ret = (hashCode*397) ^ ((hash*397) ^ count);
#if DEBUG
                Debug.WriteLine("" + ret + " <- CombineHashCodeForOrderNoMatterList<" + typeof(T) + ">(" + arg + ")");
#endif
                return ret;
            }
        }

        /// <summary>
        /// Get a hashcode using a fluent interface like this:<br />
        /// return 0.CombineHashCode(field1).CombineHashCode(field2).
        ///     CombineHashCode(field3);
        /// Use same logic as Resharper generate GetHashcode method
        /// </summary>
        public static int CombineHashCode<T>(this int hashCode, T arg)
        {
            unchecked
            {
                int hash;
                if (arg == null)
                {
                    hash = 0;
                }
                else
                {
                    if (typeof (T) == typeof (String))
                    {
                        hash = "" == arg.ToString() ? 0 : arg.GetHashCode();
                    }
                    else
                    {
                        hash = arg.GetHashCode();
                    }
                }
                var ret = (hashCode*397) ^ hash;
#if DEBUG
                Debug.WriteLine("" + ret + " <- CombineHashCode<" + typeof(T) + ">(" + arg + ")");
#endif
                return ret;
            }
        }

        /// <summary>
        /// Get a hashcode of InnerText of HTML
        /// </summary>
        public static int CombineHashCodeHtml(this int hashCode, String html)
        {
            unchecked
            {
                int hash;
                if (String.IsNullOrEmpty(html))
                {
                    hash = 0;
                }
                else
                {
                    if (!html.Contains("<body>") || html.Contains("</body>"))
                    {
                        html = "<body>" + html + "</body>";
                    }
                    HtmlDocument doc = new HtmlDocument();
                    doc.LoadHtml(html);
                    string text = doc.DocumentNode.SelectSingleNode("//body").InnerText;
                    text = Regex.Replace(text, @"\s+", " ").Trim();
                    text = text.Replace("&nbsp;", "");
#if DEBUG
                    Debug.WriteLine("CombineHashCodeHtml: InnerText [" + text + "]");
#endif
                    hash = text.GetHashCode();
                }
                var ret = (hashCode * 397) ^ hash;
#if DEBUG
                Debug.WriteLine("" + ret + " <- CombineHashCodeHtml()");
#endif
                return ret;
            }
        }
    }
}