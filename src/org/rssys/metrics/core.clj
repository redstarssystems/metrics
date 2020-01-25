(ns org.rssys.metrics.core
  (:require [io.pedestal.log :as log])
  (:import (java.lang.management ManagementFactory)
           (java.util.concurrent TimeUnit)
           (com.codahale.metrics MetricRegistry ConsoleReporter Slf4jReporter)
           (java.lang.management ManagementFactory)
           (org.slf4j LoggerFactory)
           (com.codahale.metrics.jmx JmxReporter)
           (org.rssys StatsDReporter)
           (clojure.lang IFn)))

;; current runtime
(def rt (Runtime/getRuntime))

;; mem units
(def mem-units {:b  1
                :kb 1024
                :mb (* 1024 1024)
                :gb (* 1024 1024 1024)})

(defn free-jvm-mem
  "# Get the amount of free memory in the Java Virtual Machine.
   * Params:
  	  **`{:keys [unit]}`** - optional parameter from mem-units, default value is :mb
   * Example:
      (free-jvm-mem) and (free-jvm-mem :unit :mb) returns free memory in megabytes.
   * Returns:
  	  _Long_  - amount of free memory in bytes or in memory units."
  [& {:keys [unit] :or {unit :mb}}]
  (quot (.freeMemory rt) (get mem-units unit 1)))

(defn total-jvm-mem
  "# Returns the total amount of memory in the Java virtual machine.
     The value returned by this method may vary over time, depending on the host environment.
   * Params:
      **`{:keys [unit]}`** - optional parameter from mem-units, default value is :mb
   * Example:
      (total-jvm-mem) and (total-jvm-mem :unit :mb) returns total memory in megabytes.
   * Returns:
  	  _Long_ - amount of total memory in bytes or in memory units."
  [& {:keys [unit] :or {unit :mb}}]
  (quot (.totalMemory rt) (get mem-units unit 1)))

(defn max-jvm-mem
  "# Returns the maximum amount of memory that the Java virtual machine will attempt to use.
   * Params:
      **`{:keys [unit]}`** - optional parameter from mem-units, default value is :mb
   * Example:
      (max-jvm-mem) and (max-jvm-mem :unit :mb) returns max memory in megabytes.
   * Returns:
  	  _Long_ - amount of max memory in bytes or in memory units."
  [& {:keys [unit] :or {unit :mb}}]
  (quot (.maxMemory rt) (get mem-units unit 1)))

(defn used-jvm-mem
  "# Detect current amount of memory used by Java virtual machine.
   * Params:
  	  **`{:keys [unit]}`** - optional parameter from mem-units, default value is :mb
   * Returns:
  	  _Long_ - amount of used memory in bytes or in memory units."
  [& {:keys [unit] :or {unit :mb}}]
  (- (total-jvm-mem :unit unit) (free-jvm-mem :unit unit)))

;; current CPU number available for JVM.
(def avail-processors (.availableProcessors rt))

(defn active-threads
  "# Detect an estimate of the number of active threads in the current thread's thread group and its subgroups.
 * Params:
    no params.
 * Returns:
    _Long_  - number of active threads."
  []
  (Thread/activeCount))

(defn- rounding-2
  "rounding number up two signs after floating point."
  [number]
  (/ (Math/round (* 100.0 number)) 100.0))

(defn system-cpu-load
  "# Detect current CPU load in system.
  * Params:
    no params.
  * Returns:
    _Float_  - current CPU load in system from 0.0.. N."
  []
  (-> (ManagementFactory/getOperatingSystemMXBean)
    .getSystemCpuLoad
    rounding-2))

(defn process-cpu-load
  "# Detect current CPU load by process.
  * Params:
    no params.
  * Returns:
    _Float_  - current CPU load by process from 0.0.. N."
  []
  (-> (ManagementFactory/getOperatingSystemMXBean)
    .getProcessCpuLoad
    rounding-2))

(defn max-file-descriptors
  "# Detect number of max file descriptors.
   * Params:
  	  no params.
   * Returns:
  	  _value_ - _Long_ number of max file descriptors."
  []
  (.getMaxFileDescriptorCount
    (ManagementFactory/getOperatingSystemMXBean)))

(defn open-file-descriptors
  "# Detect number of open file descriptors.
   * Params:
  	  no params.
   * Returns:
  	  _value_ - _Long_ number of open file descriptors."
  []
  (.getOpenFileDescriptorCount
    (ManagementFactory/getOperatingSystemMXBean)))


(defn total-swap-size
  "# Detect total swap size.
   * Params:
  	  **`{:keys [unit]}`** - optional parameter from mem-units, default value is :mb
   * Returns:
  	  _value_ - _Long_ number of swap size in bytes or in memory units."
  [& {:keys [unit] :or {unit :mb}}]
  (quot (.getTotalSwapSpaceSize (ManagementFactory/getOperatingSystemMXBean))
    (get mem-units unit 1)))

(defn free-swap-size
  "# Detect free swap size.
   * Params:
  	  **`{:keys [unit]}`** - optional parameter from mem-units, default value is :mb
   * Returns:
  	  _value_ - _Long_ number of free swap size in bytes or in memory units."
  [& {:keys [unit] :or {unit :mb}}]
  (quot (.getFreeSwapSpaceSize (ManagementFactory/getOperatingSystemMXBean))
    (get mem-units unit 1)))

(defn metric-registry
  "# Create new instance of `com.codahale.metrics.MetricRegistry`.
   * Params:
  	  no params.
   * Returns:
  	  _`com.codahale.metrics.MetricRegistry`_ instance."
  []
  (io.pedestal.log/metric-registry))

(defn console-reporter
  "# Create console reporter for given registry and start it.
   * Params:
  	  **`registry`**      - `com.codahale.metrics.MetricRegistry` instance.
  	  **`report-period`** - _Long_ number of units for report period. (e.g. 10)
  	  **`report-units`**  - `java.util.concurrent.TimeUnit` instance (e.g. TimeUnit/SECONDS)
   * Returns:
  	  _nil_."
  [^MetricRegistry registry report-period report-units]
  (doto (some-> (ConsoleReporter/forRegistry registry)
          (.outputTo System/out)
          (.convertRatesTo TimeUnit/SECONDS)
          (.convertDurationsTo TimeUnit/MILLISECONDS)
          (.build))
    (.start report-period report-units))
  nil)


(defn slf4j-reporter
  "# Create SLF4j reporter for given registry and start it.
  Logger name is \"org.rssys.metrics\".
   * Params:
  	  **`registry`**      - `com.codahale.metrics.MetricRegistry` instance.
  	  **`report-period`** - _Long_ number of units for report period. (e.g. 10)
  	  **`report-units`**  - `java.util.concurrent.TimeUnit` instance (e.g. TimeUnit/SECONDS)
   * Returns:
  	  _nil_."
  [^MetricRegistry registry report-period report-units]
  (doto (some-> (Slf4jReporter/forRegistry registry)
          (.outputTo (LoggerFactory/getLogger "org.rssys.metrics"))
          (.convertRatesTo TimeUnit/SECONDS)
          (.convertDurationsTo TimeUnit/MILLISECONDS)
          (.build))
    (.start report-period report-units))
  nil)


(defn jmx-reporter
  "# Create JMX reporter for given registry and start it.
  Domain name is \"org.rssys.metrics\".

  Example:
  1. Run java app with args: -Dcom.sun.management.jmxremote.port=10999
                             -Dcom.sun.management.jmxremote.authenticate=false
                             -Dcom.sun.management.jmxremote.ssl=false
  2. Attach jconsole: jconsole localhost:10999
  3. See domain - org.rssys.metrics

   * Params:
  	  **`registry`**      - `com.codahale.metrics.MetricRegistry` instance.
   * Returns:
  	  _nil_."
  [^MetricRegistry registry]
  (doto (some-> (JmxReporter/forRegistry registry)
          (.inDomain "org.rssys.metrics")
          (.build))
    (.start))
  nil)

(defn statsd-reporter
  "# Create StatsD reporter for given registry and start it.
   * Params:
  	  **`registry`**      - `com.codahale.metrics.MetricRegistry` instance.
  	  **`report-period`** - _Long_ number of units for report period. (e.g. 10)
  	  **`report-units`**  - `java.util.concurrent.TimeUnit` instance (e.g. TimeUnit/SECONDS)
  	  **`host`**          - `String` hostname or ip of StatsD server.
  	  **`port`**          - `Long` port of StatsD server.
   * Returns:
  	  _nil_."
  [^MetricRegistry registry report-period report-units host port]
  (doto (some-> (StatsDReporter/forRegistry registry)
          (.convertRatesTo TimeUnit/SECONDS)
          (.convertDurationsTo TimeUnit/MILLISECONDS)
          (.build host port))
    (.start report-period report-units))
  nil)


(defmacro counter
  "Update a single Numeric/Long metric by the `delta` amount."
  [registry metric-name ^Long delta]
  `(io.pedestal.log/counter ~registry (io.pedestal.log/format-name ~metric-name) ~delta))


(defmacro gauge
  "Register a single metric value, returned by a 0-arg function.
  This function will be called everytime the Guage value is requested."
  [registry metric-name ^IFn value-fn]
  `(io.pedestal.log/gauge ~registry (io.pedestal.log/format-name ~metric-name) ~value-fn))


(defmacro histogram
  "Measure a distribution of Long values."
  ([registry metric-name ^Long value]
   `(io.pedestal.log/histogram ~registry (io.pedestal.log/format-name ~metric-name) ~value)))

(defmacro meter
  "Measure the rate of a ticking metric - a meter."
  ([registry metric-name ^Long n-events]
   `(io.pedestal.log/meter ~registry (io.pedestal.log/format-name ~metric-name) ~n-events)))


(comment
  (def r (metric-registry))                                 ;; create metrics registry

  ;;create reporters
  (console-reporter r 10 TimeUnit/SECONDS)                  ;;report to console every 10 sec
  (slf4j-reporter r 15 TimeUnit/SECONDS)                    ;;report to logger every 15 sec
  (jmx-reporter r)                                          ;;expose current metrics via JMX
  (statsd-reporter r 10 TimeUnit/SECONDS "127.0.0.1" 8125)  ;; report to StatsD server via UDP every 10 sec

  ;; create metrics, gather them in registry and emit them via reporters
  (counter r "counter1" 5)
  (gauge r "gauge1" used-jvm-mem)
  (histogram r "hist1" 1177)
  (meter r "meter1" 43)
  )