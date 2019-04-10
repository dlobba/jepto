JEpTO
=====

* actors' log file are created in the user's home directory
  in the `EpTOlogs` folder.

* it is possible to instantiate an individual actor
  using maven and providing a valid configuration file in `src/main/resources`

  Invoke the actor with the following

  ```
  mvn exec:java@network -Dconfig.resource=tracker_0.conf
  ```

* to run a single threaded execution, defined by the `EptoMain` class,
  run the following:

  ```
  mvn exec:java@single -Dactors.num=20
  ```

## Single threaded execution

The single threaded execution is the most featureful option to test JEpTo.
With this, it is possible to define with a configuration file all the system
parameters describing the program behaviour.


To load a configuration file, execute the following within the `actors` folder
starting from the project root:

`mvn exec:java@single -Drun.config=<path-to-conf-file>`

Alternatively, it is possible to directly use a configuration file defined within
the classpath (in the `actors/src/main/resources` folder), by simply
issuing the same command with just the name of the configuration file, without stating the
complete path.  
For instance, to run the program with the configuration defined within the `run.conf`
file, execute:

`mvn exec:java@single -Drun.config=run.conf`

It is possible to find a skeleton configuration file at `actors/src/main/resources/run.conf`:
```
# Test with n=100 peers and c=3
#
# K = ceil( (2 e ln n) / (ln ln n) )
# ttl =  2 * ceil( (c+1) log_2 n ) + 1
akka {
    log-dead-letters-during-shutdown = off
    log-dead-letters = off
}
jepto.config {
    cyclon {
        view-size       = 100
        shuffle-length  = 30
        shuffle-period-millis= 100
    }
    num-receivers   = 17
    max-ttl         = 43
    round-interval  = 5000
    num-actors      = 100
    as-paper        = false
    # define the simulation time in seconds
    # (optional) if not defined, the system keeps going
    sim-time        = 20
    # (option) if not defined, backup to "INFO"
    log-level       = "INFO"
}
```

## Logs Levels

The program heavily relies on loggers. These are effectively used to give
insights on the algorithm functioning, from which all our data analysis
has been based upon.

Namely, logs are used for two specific purposes:

1. collect data to analyse events/packets statistics, such as
   delivery rates, broadcasts, total\_order...

2. collect data on actor-specific data structure contents across
   different phases of the algorithm.

   In particular, the `received` and `deliverable` sets
   content, for each actor, have been logged to console.

Within the program, two logging levels are defined:

* INFO, used to satisfy purpose (1)
* DEBUG, used to satisfy purpose (2)

Note that, by enabling the DEBUG log, INFO logs are displayed too!
This generates an enormous amount of data, which is not required
for the performance evaluation of the algorithm.

These two log levels can be set in the configuration file
only when using the single-threaded execution, as
described in the sample configuration [previously described](#single-threaded-execution).
