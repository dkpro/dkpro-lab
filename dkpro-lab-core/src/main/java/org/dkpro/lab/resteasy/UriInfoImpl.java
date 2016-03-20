/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.

 * The original file does not contain a license statement. The license file in the projects
 * top-level directory states the files are under the Apache License 2.0:
 * 
 *   http://resteasy.svn.sourceforge.net/viewvc/resteasy/tags/RESTEASY_JAXRS_2_2_3_GA/
 *   License.html?revision=1542&content-type=text%2Fplain
 * 
 * Original location: 
 * 
 *   http://resteasy.svn.sourceforge.net/viewvc/resteasy/tags/RESTEASY_JAXRS_2_2_3_GA/
 *   resteasy-jaxrs/src/main/java/org/jboss/resteasy/specimpl/UriInfoImpl.java
 *   ?revision=1542&content-type=text%2Fplain 
 */
package org.dkpro.lab.resteasy;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UriInfoImpl implements UriInfo
{
   private final String path;
   private final String encodedPath;
   private MultivaluedMap<String, String> queryParameters;
   private MultivaluedMap<String, String> encodedQueryParameters;
   private MultivaluedMap<String, String> pathParameters;
   private MultivaluedMap<String, String> encodedPathParameters;
   private MultivaluedMap<String, PathSegment[]> pathParameterPathSegments;
   private MultivaluedMap<String, PathSegment[]> encodedPathParameterPathSegments;

   private final List<PathSegment> pathSegments;
   private final List<PathSegment> encodedPathSegments;
   private final URI absolutePath;
   private URI absolutePathWithQueryString;
   private final URI baseURI;
   private List<String> matchedUris;
   private List<String> encodedMatchedUris;
   private List<Object> ancestors;


   public UriInfoImpl(URI absolutePath, URI baseUri, String encodedPath, String queryString, List<PathSegment> encodedPathSegments)
   {
      /*
      logger.info("**** URIINFO encodedPath: " + encodedPath);
      for (PathSegment segment : encodedPathSegments)
      {
         logger.info("   Segment: " + segment.getPath());
      }
      */
      this.encodedPath = encodedPath;
      this.path = Encode.decodePath(encodedPath);
      //System.out.println("path: " + path);
      //System.out.println("encodedPath: " + encodedPath);

      this.absolutePath = absolutePath;
      this.encodedPathSegments = encodedPathSegments;
      this.baseURI = baseUri;

      extractParameters(queryString);
      this.pathSegments = new ArrayList<PathSegment>(encodedPathSegments.size());
      for (PathSegment segment : encodedPathSegments)
      {
         pathSegments.add(new PathSegmentImpl(((PathSegmentImpl) segment).getOriginal(), true));
      }
      if (queryString == null)
      {
         this.absolutePathWithQueryString = absolutePath;
      }
      else
      {
         this.absolutePathWithQueryString = URI.create(absolutePath.toString() + "?" + queryString);
      }
   }

   @Override
public String getPath()
   {
      return path;
   }

   @Override
public String getPath(boolean decode)
   {
      if (decode) return getPath();
      return encodedPath;
   }

   @Override
public List<PathSegment> getPathSegments()
   {
      return pathSegments;
   }

   @Override
public List<PathSegment> getPathSegments(boolean decode)
   {
      if (decode) return getPathSegments();
      return encodedPathSegments;
   }

   @Override
public URI getRequestUri()
   {
      return absolutePathWithQueryString;
   }

   @Override
public UriBuilder getRequestUriBuilder()
   {
      return UriBuilder.fromUri(absolutePathWithQueryString);
   }

   @Override
public URI getAbsolutePath()
   {
      return absolutePath;
   }

   @Override
public UriBuilder getAbsolutePathBuilder()
   {
      return UriBuilder.fromUri(absolutePath);
   }

   @Override
public URI getBaseUri()
   {
      return baseURI;
   }

   @Override
public UriBuilder getBaseUriBuilder()
   {
      return UriBuilder.fromUri(baseURI);
   }

   @Override
public MultivaluedMap<String, String> getPathParameters()
   {
      if (pathParameters == null)
      {
         pathParameters = new MultivaluedMapImpl<String, String>();
      }
      return pathParameters;
   }

   public void addEncodedPathParameter(String name, String value)
   {
      getEncodedPathParameters().add(name, value);
      String value1 = Encode.decodePath(value);
      getPathParameters().add(name, value1);
   }

   private MultivaluedMap<String, String> getEncodedPathParameters()
   {
      if (encodedPathParameters == null)
      {
         encodedPathParameters = new MultivaluedMapImpl<String, String>();
      }
      return encodedPathParameters;
   }

   public MultivaluedMap<String, PathSegment[]> getEncodedPathParameterPathSegments()
   {
      if (encodedPathParameterPathSegments == null)
      {
         encodedPathParameterPathSegments = new MultivaluedMapImpl<String, PathSegment[]>();
      }
      return encodedPathParameterPathSegments;
   }

   public MultivaluedMap<String, PathSegment[]> getPathParameterPathSegments()
   {
      if (pathParameterPathSegments == null)
      {
         pathParameterPathSegments = new MultivaluedMapImpl<String, PathSegment[]>();
      }
      return pathParameterPathSegments;
   }

   @Override
public MultivaluedMap<String, String> getPathParameters(boolean decode)
   {
      if (decode) return getPathParameters();
      return getEncodedPathParameters();
   }

   @Override
public MultivaluedMap<String, String> getQueryParameters()
   {
      if (queryParameters == null)
      {
         queryParameters = new MultivaluedMapImpl<String, String>();
      }
      return queryParameters;
   }

   protected MultivaluedMap<String, String> getEncodedQueryParameters()
   {
      if (encodedQueryParameters == null)
      {
         this.encodedQueryParameters = new MultivaluedMapImpl<String, String>();
      }
      return encodedQueryParameters;
   }


   @Override
public MultivaluedMap<String, String> getQueryParameters(boolean decode)
   {
      if (decode) return getQueryParameters();
      else return getEncodedQueryParameters();
   }

   protected void extractParameters(String queryString)
   {
      if (queryString == null || queryString.equals("")) return;

      String[] params = queryString.split("&");

      for (String param : params)
      {
         if (param.indexOf('=') >= 0)
         {
            String[] nv = param.split("=");
            try
            {
               String name = URLDecoder.decode(nv[0], "UTF-8");
               String val = nv.length > 1 ? nv[1] : "";
               getEncodedQueryParameters().add(name, val);
               getQueryParameters().add(name, URLDecoder.decode(val, "UTF-8"));
            }
            catch (UnsupportedEncodingException e)
            {
               throw new RuntimeException(e);
            }
         }
         else
         {
            try
            {
               String name = URLDecoder.decode(param, "UTF-8");
               getEncodedQueryParameters().add(name, "");
               getQueryParameters().add(name, "");
            }
            catch (UnsupportedEncodingException e)
            {
               throw new RuntimeException(e);
            }
         }
      }
   }

   @Override
public List<String> getMatchedURIs(boolean decode)
   {
      if (decode)
      {
         if (matchedUris == null) matchedUris = new ArrayList<String>();
         return matchedUris;
      }
      else
      {
         if (encodedMatchedUris == null) encodedMatchedUris = new ArrayList<String>();
         return encodedMatchedUris;
      }
   }

   @Override
public List<String> getMatchedURIs()
   {
      return getMatchedURIs(true);
   }

   @Override
public List<Object> getMatchedResources()
   {
      if (ancestors == null) ancestors = new ArrayList<Object>();
      return ancestors;
   }


   public void pushCurrentResource(Object resource)
   {
      if (ancestors == null) ancestors = new ArrayList<Object>();
      ancestors.add(0, resource);
   }

   public void popCurrentResource()
   {
      if (ancestors != null && ancestors.size() > 0)
      {
         ancestors.remove(0);
      }
   }

   public void pushMatchedURI(String encoded, String decoded)
   {
      if (encodedMatchedUris == null) encodedMatchedUris = new ArrayList<String>();
      encodedMatchedUris.add(0, encoded);

      if (matchedUris == null) matchedUris = new ArrayList<String>();
      matchedUris.add(0, decoded);
   }

   public void popMatchedURI()
   {
      if (encodedMatchedUris != null && encodedMatchedUris.size() > 0)
      {
         encodedMatchedUris.remove(0);
      }
      if (matchedUris != null && matchedUris.size() > 0)
      {
         matchedUris.remove(0);
      }
   }

}