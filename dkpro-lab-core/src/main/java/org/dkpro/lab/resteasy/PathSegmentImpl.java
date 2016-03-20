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
 *   resteasy-jaxrs/src/main/java/org/jboss/resteasy/specimpl/PathSegmentImpl.java
 *   ?revision=1464&content-type=text%2Fplain
 */
package org.dkpro.lab.resteasy;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PathSegmentImpl implements PathSegment
{
   private String path;
   private final String original;
   private final MultivaluedMap<String, String> matrixParameters = new MultivaluedMapImpl<String, String>();

   /**
    * @param segment encoded path segment
    * @param decode whether or not to decode values
    */
   public PathSegmentImpl(String segment, boolean decode)
   {
      this.original = segment;
      this.path = segment;
      int semicolon = segment.indexOf(';');
      if (semicolon >= 0)
      {
         if (semicolon > 0) this.path = segment.substring(0, semicolon);
         else this.path = "";
         String matrixParams = segment.substring(semicolon + 1);
         String[] params = matrixParams.split(";");
         for (String param : params)
         {
            String[] namevalue = param.split("=");
            if (namevalue != null && namevalue.length > 0)
            {
               String name = namevalue[0];
               if (decode) name = Encode.decodePath(name);
               String value = "";
               if (namevalue.length > 1)
               {
                  value = namevalue[1];
               }
               if (decode) value = Encode.decodePath(value);
               matrixParameters.add(name, value);
            }
         }
      }
      if (decode) this.path = Encode.decodePath(this.path);
   }

   public String getOriginal()
   {
      return original;
   }

   @Override
public String getPath()
   {
      return path;
   }

   @Override
public MultivaluedMap<String, String> getMatrixParameters()
   {
      return matrixParameters;
   }

   @Override
public String toString()
   {
      StringBuffer buf = new StringBuffer();
      if (path != null) buf.append(path);
      if (matrixParameters != null)
      {
         for (String name : matrixParameters.keySet())
         {
            for (String value : matrixParameters.get(name))
            {
               buf.append(";").append(name).append("=").append(value);
            }
         }
      }
      return buf.toString();
   }

   /**
    *
    * @param path encoded full path
    * @param decode whether or not to decode each segment
    */
   public static List<PathSegment> parseSegments(String path, boolean decode)
   {
      List<PathSegment> pathSegments = new ArrayList<PathSegment>();

      if (path.startsWith("/")) path = path.substring(1);
      String[] paths = path.split("/");
      for (String p : paths)
      {
         pathSegments.add(new PathSegmentImpl(p, decode));
      }
      return pathSegments;
   }

}