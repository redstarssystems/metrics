# Metrics

Metrics library designed to perform application monitoring.  
Based on [Coda Hale Metrics](https://metrics.dropwizard.io/4.1.2/)
and [pedestal.log](https://github.com/pedestal/pedestal/tree/master/log).  
Library allows to expose metrics via StatsD, JMX, console, logs (slf4j).

## Usage

For Leiningen add to project.clj: ```[org.rssys/metrics "0.1.0"]```

For Deps CLI add to deps.edn:  ```{:deps {org.rssys/metrics {:mvn/version "0.1.0}}}```

Import necessary namespaces:

```clojure
(require '[org.rssys.metrics.core :as metrics])
(import '(java.util.concurrent TimeUnit))
```

### Basic metrics

- **counter** - update a single Numeric/Long metric by the `delta` amount.  

- **gauge** - register a single metric value with a 0-arg function (gauge is returned by a 0-arg function.)  
This function will be called everytime the guage value is requested.

- **histogram** - measure a distribution of Long values.

- **meter** - measure the rate of a ticking metric - a meter.

### Built-in functions

These functions returns various OS and JVM parameters which may be exposed via metrics.

- **(metrics/free-jvm-mem)** - Get the amount of free memory in the Java Virtual Machine.
- **(metrics/total-jvm-mem)** - Returns the total amount of memory in the Java virtual machine.  
The value returned by this method may vary over time, depending on the host environment.
- **(metrics/max-jvm-mem)** - Returns the maximum amount of memory that the Java virtual machine will attempt to use.
- **(metrics/used-jvm-mem)** - Detect current amount of memory used by Java virtual machine.
- **metrics/avail-processors** - value of current CPU number available for JVM. Value detected at JVM start.
- **(metrics/active-threads)** - Detect an estimate of the number of active threads in the current thread's thread group and its subgroups.
- **(metrics/system-cpu-load)** - Detect current CPU load in system.
- **(metrics/process-cpu-load)** - Detect current CPU load by JVM process.
- **(metrics/max-file-descriptors)** - Detect number of max file descriptors.
- **(metrics/open-file-descriptors)** - Detect number of open file descriptors.
- **(metrics/total-swap-size)** - Detect total swap size.
- **(metrics/free-swap-size)** - Detect free swap size.

### JMX

To access metrics via JMX 

1. Run java app with args:  
``` -Dcom.sun.management.jmxremote.port=10999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false```  
                             
2. Attach jconsole: jconsole localhost:10999  

3. See domain - org.rssys.metrics  
  
### Code Example 

```clojure
(def r (metrics/metric-registry))                                   ;; create metrics registry

  ;;create reporters
  (metrics/console-reporter r 10 TimeUnit/SECONDS)                  ;; report to console every 10 sec
  (metrics/slf4j-reporter r 15 TimeUnit/SECONDS)                    ;; report to logger every 15 sec
  (metrics/jmx-reporter r)                                          ;; expose current metrics via JMX
  (metrics/statsd-reporter r 10 TimeUnit/SECONDS "127.0.0.1" 8125)  ;; report to StatsD server via UDP every 10 sec

  ;; create metrics, gather them in registry and emit them via reporters
  (metrics/counter r "counter1" 5)
  (metrics/gauge r "gauge1" metrics/used-jvm-mem)                   
  (metrics/histogram r "hist1" 1177)
  (metrics/meter r "meter1" 43)
```

## Build this library

### Compile Java classes

```bash
$ clojure -R:bg -A:javac
```

### REPL

```bash
$ clojure -A:repl
nREPL server started on port 56785 on host localhost - nrepl://localhost:56785
```
or, if you want to compile sources from repl, then include :bg alias

```bash
$ clojure -R:bg -A:repl
nREPL server started on port 56788 on host localhost - nrepl://localhost:56788
```

### Tests

To run all tests:
```bash
$ clojure -A:test
```

To run unit tests only:
```bash
$ clojure -A:test :unit
```

### Package jar

```bash
$ clojure -R:bg -A:jar
```

### Local install

To install jar to local .m2 :

```bash
$ clojure -R:bg -A:install
```

### Deploy to clojars

Put your clojars.org credentials to settings.xml (or uncomment login and password prompt in dev/src/build.clj).

```bash
$ clojure -R:bg -A:deploy
```
This command will sign jar before deploy, using your gpg key. (see dev/src/build.clj for signing options)

## License

Copyright © 2020 Mike Ananev 

Distributed under the Eclipse Public License 2.0 or (at your option) any later version.
