EpTO
====

* actors' log file are created in the user's home directory
  in the `EpTOlogs` folder.

* it is possible to instatiate an individual actor
  using maven and providing a valid config file in `src/main/resources`

  Invoke the actor with the following

  ```
  mvn exec:java -Dconfig.resource=tracker_0.conf
  ```
