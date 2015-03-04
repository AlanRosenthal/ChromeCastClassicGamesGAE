ChromeCastClassicGamesGAE
=========================

The backend server running on Google App Engine

* Install Apache Maven


* Update file ./src/main/webapp/WEB-INF/appengine-web.xml
 * Replace your-app-id with your App Engine ID
 * eg: your-app-id -> c-three-games-test


* Update file ./src/main/java/com/appspot/c_three_games/Constants.java
 * GCM_KEY needs to be set to your private GCM key.
 * http://developer.android.com/google/gcm/gs.html
 * Follow steps "Creating a Google API project", "Enabling the GCM Service", and "Obtaining an API Key"
 * Don't commit your key to a public git


* Run Dev Server
 * mvn appengine:devserver


* Deploy to App Engine
 * mvn appengine:update


* Build Android Endpoints JAR
 * https://cloud.google.com/appengine/docs/java/endpoints/gen_clients
 * mvn clean install appengine:endpoints_get_client_lib
 * cd ./target/endpoints-client-libs/warAPI/
 * mvn install
 * file: ./target/warAPI-v1-1.19.1-SNAPSHOT.jar
