(ns cliopatra.impl
  (:require [clojure.string :as str]))


(def usage-exception-cause (Throwable. "usage-exception"))

(defn throw-usage-exception [msg]
  (throw (RuntimeException. msg usage-exception-cause)))

(defn format-missing [missing]
  (format "missing required arguments: %s\n"
          (str/join ", " (map name missing))))

(defn required-option? [option]
  (-> (drop-while (partial not= :required) option) next first))

(defn trunc-multi-word-string [s]
  (if (re-find #"\s+" s)
    (first (str/split s #"\s+"))
    s))

(defn opt-spec->opt-name-str [op]
  (-> (take-while #(= \- (first %)) op)
      last
      (str/replace #"^-*" "")
      trunc-multi-word-string))

(defn required-options [options]
  (keep (fn [op] (when (required-option? op)
                  (-> (opt-spec->opt-name-str op)
                      keyword)))
        options))

(defn opts-spec-keys [opts-specs]
  (for [op opts-specs]
    (->> (opt-spec->opt-name-str op)
         (#(str/replace % #"^\[no\-\]" ""))
         symbol)))

(def ^:dynamic *env* nil)

(defmacro with-environment [env & body]
  `(binding [*env* ~env]
     ~@body))

(defn get-environment [& ks]
  (get-in *env* ks))

(defn get-commands [namespaces]
  (for [ns' namespaces
        [sym var*] (ns-interns ns')
        :when (:cliopatra.command/command (meta var*))]
    var*))

(defn get-command [namespaces cmd]
  (first (for [ns' namespaces
               [sym var*] (ns-interns ns')
               :when (:cliopatra.command/command (meta var*))
               :when (= (str sym) cmd)]
           var*)))