Copyright 2018 Martin Winandy

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.

-------------------------------------------
   Using tinylog's JBoss Logging binding
-------------------------------------------

jboss-tinylog.jar is a binding for JBoss Logging 3.

The following JARs are required in the classpath:

  - jboss-logging.jar (https://search.maven.org/search?q=a:jboss-logging)
  
  - jboss-tinylog.jar
  
  - tinylog-api.jar (https://tinylog.org/v2/download/)
  
  - tinylog-impl.jar or any other implementation

Log entries are forwarded from jboss-logging.jar to tinylog-api.jar and
processed by the tinylog implementation like other log entries.
