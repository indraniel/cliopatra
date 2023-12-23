(ns cliopatra.command
  (:require [cliopatra.impl :as impl]
            [clojure.set :as set]
            [clojure.tools.cli :as cli]))

(defmacro defcommand
  "Defines a var with a function for use with cliopatra.command/delegate.
   Var has :cliopatra.command/command, :cliopatra.command/name,
   :cliopatra.command/usage, and :cliopatra.command/opts-spec metadata.

  Usage:

     (defcommand name-of-command
       \"documentation string for the command definition\"
       {;; usage-heading defaults to command's docstring if not provided
        :usage-heading \"documentation for command, prints above usage banner\"

        ;; opts-spec = tools.cli opts spec + added required opts feature
        :opts-spec [[\"-f\" \"--foo\" ... :required true]
                    [\"-b\" \"--bar\"]]

        ;; bind-args-to is for positional non-option args. defaults to: args
        :bind-args-to [baz quux]}
      ,,, body ,,,)

      In the body of defcommand, these lexical bindings are made available:
        usage (the usage heading and banner generated by tools.cli),
        opts (a map of options and their values),
        a lexical binding for each sym in bind-args-to (or args if unspecified),
        and a lexical binding for each opt in opts-spec"
  [name doc-string specs & body]
  (let [{:keys [usage-heading opts-spec bind-args-to] :as specs} specs
        opts-spec' (eval opts-spec)]
    (assert (and (sequential? opts-spec')
                 (every? sequential? opts-spec'))
            "opts-spec should be a seq of seqs!")

    `(defn ~(vary-meta name merge {::command true
                                   ::name name
                                   ::usage (or usage-heading doc-string)
                                   ::opts-spec opts-spec'})
       [args#]
       (let [assembled-opts-spec#  (conj ~opts-spec ["-h" "--[no-]help" "Print help"])
             output-of-parse-opts# (cli/parse-opts (vec args#) assembled-opts-spec#)
             parsed-opts#          (:options output-of-parse-opts#)
             arg-values#           (:arguments output-of-parse-opts#)
             usage-banner#         (:summary output-of-parse-opts#)
             errors#               (:errors output-of-parse-opts#)
             usage#                (format "%s\n\n%s" ~(or usage-heading doc-string) usage-banner#)]
         (comment tap> (format "parsed-opts#: %s" parsed-opts#))
         (comment tap> (format "arg-values#: %s" arg-values#))
         (try
           (if-let [missing#
                    (seq
                     (set/difference
                      (set (impl/required-options ~opts-spec))
                      (set (keys parsed-opts#))))]
             (impl/throw-usage-exception (impl/format-missing missing#))
             (if (:help parsed-opts#)
               (println usage#)
               (let [~'usage usage#
                     ~(or bind-args-to '[& _]) arg-values#
                     {:keys [~@(impl/opts-spec-keys opts-spec')]
                      :as ~'opts} parsed-opts#]
                 ~@body)))
           (catch RuntimeException ex#
             (if (= (.getCause ex#) impl/usage-exception-cause)
               (do
                 (println "Error:" (.getMessage ex#))
                 (println usage#))
               (throw ex#))))))))

(defn- help [namespaces & [c]]
  (if-let [cmd-var (impl/get-command namespaces c)]
    (do
      (cmd-var ["--help"])
      (println))
    (do
      (if c
        (println c (format "is not a valid command. Available commands:\n"))
        (println "Available commands:\n"))
      (doseq [cmd-var (impl/get-commands namespaces)]
        (println (format "\t\t%s\t-\tUsage: %s\n"
                         (:name (meta cmd-var))
                         (:cliopatra.command/usage (meta cmd-var))))))))

(defn dispatch
  "Dispatch command-line args to a matching defcommand in any of the listed
   namespaces.

   Usage:

     (dispatch 'my-lib.main args)
     (dispatch ['my-lib.commands1 my-lib.commands2] args)"
  [namespaces args & [env]]
  (let [namespaces (if (coll? namespaces) namespaces [namespaces])]
    (impl/with-environment env
      (if-let [cmd (impl/get-command namespaces (first args))]
        (cmd (rest args))
        (help namespaces (second args))))))
