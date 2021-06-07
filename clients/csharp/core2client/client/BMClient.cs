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
using System.Collections;
using System.Collections.Specialized;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Runtime.Serialization.Json;
using System.Text;
using System.Web;
using System.Web.Script.Serialization;

namespace core2client
{
    public class BMClient
    {
        public ILogger logger;

        protected String sid;
        protected String baseUrl;
        protected String version;

        /*
     * Set your own logger by implementing this interface
     */
        public interface ILogger
        {
            void LogMessage(String message);
        }

        internal class DefaultLogger : ILogger
        {
            public void LogMessage(String message)
            {
                Debug.WriteLine(message);
            }
        }

        public delegate void ProgressDelegate(long bytesUploaded);
        public ProgressDelegate progressDelegate;

        public dynamic execute<T>(String path, NameValueCollection queryParams, dynamic body, String verb)
        {
            String url = this.baseUrl + path;

            UriBuilder builder = new UriBuilder(url);
            NameValueCollection query = HttpUtility.ParseQueryString(builder.Query);
            query.Add(queryParams);
            builder.Query = query.ToString();

            HttpWebRequest request = (HttpWebRequest)WebRequest.Create(builder.ToString());
            request.UserAgent = "BlueMind C# Client";
            request.Method = verb;

            this.logger.LogMessage("Send [" + request.Method + "] " + builder.ToString());

            ServicePointManager.ServerCertificateValidationCallback +=
              (sender, certificate, chain, sslPolicyErrors) => true;

            SetSecurityProptocol();

            if (this.sid != null)
            {
                request.Headers.Add("X-BM-ApiKey", this.sid);
            }
            if (this.version != null)
            {
                request.Headers.Add("X-BM-ClientVersion", this.version);
            }

            if (body != null)
            {
                if (body is Stream)
                {
                    request.SendChunked = true;
                    request.AllowWriteStreamBuffering = false;
                    Stream dataStream = request.GetRequestStream();
                    long bytesUploaded = 0;
                    byte[] buffer = new byte[4096];
                    int bytesRead = 0;
                    while ((bytesRead = body.Read(buffer, 0, buffer.Length)) != 0)
                    {
                        dataStream.Write(buffer, 0, bytesRead);
                        dataStream.Flush();
                        if (progressDelegate != null)
                        {
                            bytesUploaded += bytesRead;
                            progressDelegate(bytesUploaded);
                        }
                    }
                    body.Close();
                    dataStream.Close();
                }
                else
                {
                    byte[] byteArray;
                    if (body is byte[])
                    {
                        this.logger.LogMessage(" byte[]");
                        request.ContentType = "application/octet-stream";
                        byteArray = body;
                    }
                    else
                    {
                        if (path.EndsWith("/loginWithParams"))
                        {
                            this.logger.LogMessage(" *****");
                        }
                        else
                        {
                            this.logger.LogMessage(" " + body);
                        }
                        request.ContentType = "application/json; charset=utf-8";
                        byteArray = Encoding.UTF8.GetBytes(body);
                    }
                    request.ContentLength = byteArray.Length;
                    Stream dataStream = request.GetRequestStream();
                    dataStream.Write(byteArray, 0, byteArray.Length);
                    dataStream.Close();
                }
            }

            using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
            {
                this.logger.LogMessage(" => [" + response.StatusCode + "]");
                String warnMessage = response.GetResponseHeader("X-BM-WarnMessage");
                if (!String.IsNullOrEmpty(warnMessage))
                {
                    this.logger.LogMessage(" " + warnMessage);
                }
                if (response.StatusCode == HttpStatusCode.NoContent)
                {
                    return null;
                }
                if (response.ContentLength == 0)
                {
                    this.logger.LogMessage(" empty");
                    return (T)GetDefaultValue(typeof(T));
                }
                using (var responseStream = response.GetResponseStream())
                {
                    dynamic res = ParseResponse<T>(responseStream, response.ContentType);
                    response.Close();
                    return res;
                }
            }
        }

        private dynamic ParseResponse<T>(Stream response, String contentType)
        {
            if (contentType == "application/json")
            {
                String json = new StreamReader(response).ReadToEnd();
                this.logger.LogMessage(" " + json);
                if (typeof(T) == typeof(String))
                {
                    return json;
                }
                if (typeof(T).IsSubclassOf(typeof(Enum)) || typeof (IDictionary).IsAssignableFrom(typeof (T)))
                {
                    return new JavaScriptSerializer().Deserialize<T>(json);
                }
                using (var ms = new MemoryStream(Encoding.UTF8.GetBytes(json)))
                {
                    DataContractJsonSerializer dcr = new DataContractJsonSerializer(typeof(T));
                    return (T)dcr.ReadObject(ms);
                }
            }
            if (typeof(T) == typeof(Stream))
            {
                this.logger.LogMessage(" Stream...");
                var data = new MemoryStream();
                response.CopyTo(data);
                return data;
            }
            this.logger.LogMessage(" byte[]");
            var content = new MemoryStream();
            response.CopyTo(content);
            return content.ToArray();
        }

        internal static object GetDefaultValue(Type t)
        {
            return t.IsValueType ? Activator.CreateInstance(t) : null;
        }

        private void SetSecurityProptocol()
        {
            //Tls12 = 3072
            ServicePointManager.SecurityProtocol = (SecurityProtocolType)3072;
            this.logger.LogMessage("SecurityProtocol: " + ServicePointManager.SecurityProtocol);
        }
    }
}
