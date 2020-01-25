(ns org.rssys.metrics.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [matcho.core :refer [match]]
            [org.rssys.metrics.core :as sut])
  (:import (java.util.concurrent TimeUnit)))

(deftest ^:unit basic-jvm-os-metrics
  (match (sut/free-jvm-mem) pos-int?)
  (match (sut/total-jvm-mem) pos-int?)
  (match (sut/max-jvm-mem) pos-int?)
  (match (sut/used-jvm-mem) pos-int?)
  (match sut/avail-processors pos-int?)
  (match (sut/active-threads) pos-int?)
  (match (sut/system-cpu-load) float?)
  (match (sut/process-cpu-load) float?)
  (match (sut/max-file-descriptors) pos-int?)
  (match (sut/open-file-descriptors) pos-int?)
  (match (sut/total-swap-size) int?)
  (match (sut/free-swap-size) int?))

(deftest ^:integration metrics-tests
  (let [r (sut/metric-registry)]

    ;;create reporters
    (sut/console-reporter r 10 TimeUnit/SECONDS)                  ;;report to console every 10 sec
    (sut/slf4j-reporter r 15 TimeUnit/SECONDS)                    ;;report to logger every 15 sec
    (sut/jmx-reporter r)                                          ;;expose current metrics via JMX
    (sut/statsd-reporter r 10 TimeUnit/SECONDS "127.0.0.1" 8125)  ;; report to StatsD server via UDP every 10 sec

    ;; create metrics, gather them in registry and emit them via reporters
    (sut/counter r "counter1" 5)
    (sut/gauge r "gauge1" sut/used-jvm-mem)
    (sut/histogram r "hist1" 1177)
    (sut/meter r "meter1" 43)
    ;; if there was no exceptions then tests are ok.

    (match (instance? com.codahale.metrics.MetricRegistry r))))