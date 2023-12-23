(ns cliopatra.core
  (:require [cliopatra.command :as command :refer [defcommand]]
            [cliopatra.impl :as impl]
            [clojure.pprint :as pp]
            [clojure.repl :as r]
            [clojure.tools.cli :as cli])
  (:gen-class))

(add-tap (bound-fn* clojure.pprint/pprint))

(defn howdy [name]
  (println (format "Hello %s" name)))

(defcommand hello
  "Create a hello subcommand"
  {:opts-spec [["-n" "--name NAME" "Name to print" :default "World"]]
   :bind-args-to [args]}
  (do
    (howdy name)
    (comment System/exit 0)))

(defn -main [& args]
  (pp/pprint (macroexpand '(defcommand hello
                             "Create a hello subcommand"
                             {:opts-spec [["-n" "--name NAME" "Name to print" :default "World"]]
                              :bind-args-to []}
                             (do
                               (howdy name)
                               (comment System/exit 0)))))
  (pp/pprint (vec args))
  (command/dispatch 'cliopatra.core args))
  

(comment
 (pp/pprint (meta #'hello))
 (r/source howdy)

 (symbol (r/demunge (-> howdy .getClass .getName)))
 (r/source-fn (-> howdy .getClass .getName clojure.repl/demunge symbol))
 (r/source-fn (-> hello .getClass .getName clojure.repl/demunge symbol))
 (macroexpand-1 '(defcommand hello
                  "Create a hello subcommand"
                  {:opts-spec [["-n" "--name NAME" "Name to print" :default "World"]
                               ["-h" "--help" "This help message."]]
                   :bind-args-to [name]}
                  (do
                    (howdy name)
                    (System/exit 0))))(pp/pprint (macroexpand-1 defcommand))
 (def cmd (impl/get-command 'cliopatra.core "hello"))
 (ns-interns 'cliopatra.core)
 (def opts-spec [["-n" "--name NAME" "Name to print" :default "World"]])
 (def args '("--name" "Yeah"))
 (def hello-subcmd (impl/get-command ['cliopatra.core] "hello"))
 (hello-subcmd args)
 (cli/parse-opts ["--name" "Yeah"] (conj opts-spec ["-h" "--[no-]help" "Print help"]))
 (impl/required-options (conj opts-spec ["-h" "--[no-]help" "Print help"])))
 
(comment
  (def flat [:a 1 :b 2 :c 3])
  (let [k (take-nth 2 flat)
        v (take-nth 2 (rest flat))]
    (zipmap k v))) ; {:a 1, :b 2, :c 3}
