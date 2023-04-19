(ns pass.app
  (:require [clojure.tools.cli :refer [parse-opts]]
            [babashka.fs :as fs]
            [table.core :as t]
            [pass.password :refer [generate-password]]
            [pass.command :refer [read-str read-cmd add-line]]
            [pass.docs :refer [docstring local-docs]]
            [pass.clipboard :refer [copy]]
            [stow.api :as stow :refer [do-cmd]]
            [stow.db :refer [store-config fetch-config delete-config]]
            [cheshire.core :refer [generate-string parse-string]]
            [fipp.edn :as fipp])
  (:import [fipp.edn EdnPrinter]))


(def db-cmds stow/cmds)

;; --------------------------------------
;; -----------     State      -----------
;; --------------------------------------

;; An atom to hold the output format
(def ^:private fmt (atom :edn))

;; An atom to 
(def ^:private default-parent (atom nil))


(defn- set-default-parent [parent]
  (if parent
    (do
      (store-config "default-parent" parent)
      (reset! default-parent (read-str (fetch-config "default-parent"))))
    (do
      (delete-config "default-parent")
      (reset! default-parent nil))))


;; --------------------------------------
;; ----------- Local commands -----------
;; --------------------------------------

(def available-cmds
  (-> (into #{} (keys db-cmds))
      (disj :init :authenticated :log-out)
      (conj :help :fmt :import :export :gen :set-default-parent :get-default-parent :copy-in-node)))


;; --- help

;; See `local-docs` in the docs namespace.


(defn db-cmd-doc [cmd]
  (if-let [var (get db-cmds cmd)]
    (docstring var)
    "no help for that."))


(defn doc
  "Returns a docstring for the cmd."
  [cmd]
  (get (local-docs {:fmt @fmt :def-parent @default-parent}) cmd
       (db-cmd-doc cmd)))


(defn- in-app-help []
  (str
   "Welcome to the pass app's help.\n"
   "-------------------------\n"
   "Available commands:\n"
   (apply str
          (map
           (fn [cmd] (str cmd "\n"))
           (sort available-cmds)))
   "For help on a command, type ':help :command-name'\n\n"
   "Additional help topics:\n"
   (apply str
          (map
           (fn [topic] (str topic "\n"))
           (sort (clojure.set/difference
                  (clojure.set/union
                   available-cmds
                   (into #{} (keys (local-docs))))
                  available-cmds))))
   "For help on a topic, type ':help :topic-name'"))


(def ^:pviate app-help
  (str
   "Welcome to pass, a minimal password manager.\n\n"
   "Usage:\n"
   "  pass <path-to-db>\n"
   "If the environment variable PASS_DB_PATH is set, you can omit the db.\n"
   "Once logged into the console, run :help or :help <:cmd-name> for more help.\n"
   "  pass -h or --help to see this help."))


;; --- output format
(defn- reset-format [format]
  (if (contains? #{:json :edn :table} format)
    (do (reset! fmt format) {:result "ok"})
    {:error "That format isn't valid."}))

;; --- import and export

(declare exec-cmd)


(defn- dupes
  "Returns the dupes in nodes."
  [nodes]
  (let [grps (group-by (juxt :parent :key) nodes)]
    (reduce (fn [acc [k v]]
              (if (> (count v) 1)
                (concat acc v)
                acc))
            []
            grps)))


(def ^:private dupes-warned? (atom false))


(defn- confirm? []
  (println "Type Y/n") (flush)
  (= "Y" (read-line)))


(defn- path-to-file [p]
  (fs/file (fs/expand-home p)))


(defn- import [f & {:keys [format] :or {format :json}}]
  {:pre [(contains? #{:json :edn} format)]}
  (try
    (let [in (try (slurp (path-to-file f))
                  (catch Exception e
                    (throw (ex-info "Couldn't find file." {}))))
          nodes (try
                  (cond
                    (= format :json)    (parse-string in keyword)
                    (= format :edn)     (read-str in))
                  (catch Exception e
                    (throw (ex-info "Couldn't parse file contents" {}))))
          nodes (if @default-parent
                  (map #(assoc % :parent @default-parent) nodes)
                  nodes)
          dupes (dupes nodes)]
      
      (if (seq dupes)
        (do (println
             (str "The following duplicates were found:\n"
                  (apply str (interpose "\n" dupes))
                  "\nYou might want to fix the them in the import file before re-running this command.\n"
                  "If you proceed now, the latter of each dupe won't be inserted.\n"
                  "Do you wish to proceed?"))
            (if (confirm?)
              (apply exec-cmd :add-nodes nodes)
              {:error "Abandonned import."}))
        (apply exec-cmd :add-nodes nodes)))
    
    (catch Exception e
      {:error (.getMessage e)})))


(defn- export [f & {:keys [format] :or {format :json}}]
  {:pre [(contains? #{:json :edn} format)]}
  (let [{:keys [error result]} (exec-cmd :get-all-nodes :decrypt? true)
        f (path-to-file f)]
   (if error
     {:error error}
     (do
       (if (= format :json)
         (spit f (generate-string result))
         (spit f (pr-str result)))
        {:result "ok"}))))


;; get all nodes under one parent and collapse for convenience



;; --------------------------------------
;; -----------    Console     -----------
;; --------------------------------------

(defn- valid-cmd?
  [cmd]
  (contains? available-cmds cmd))


(defn- copy-in-node [args]
  (let [{:keys [result error]} (apply do-cmd :get-in-node args)]
    (if error
      {:error error}

      (do (copy result)
          {:result "copied."}))))


(defn- exec-cmd
  [cmd & arguments]
  (cond
    ;; :help, :import and :fmt are local commands
    (and (= :help cmd) (seq arguments))
    {:result (doc (first arguments))}

    (= :help cmd)
    {:result (in-app-help)}

    (= :fmt cmd)
    (reset-format (first arguments))

    (= :import cmd)
    (apply import arguments)

    (= :export cmd)
    (apply export arguments)

    (= :gen cmd)
    {:result (apply generate-password arguments)}

    (= :set-default-parent cmd)
    {:result (apply set-default-parent arguments)}

    (= :get-default-parent cmd)
    {:result @default-parent}

    (= :copy-in-node cmd)
    (copy-in-node arguments)

    (not (valid-cmd? cmd))
    {:error (str cmd " isn't a valid command.")}
    
    :else
    (let [{:keys [result error]} (apply do-cmd cmd arguments)]
      (cond
        error              {:error error}

        (integer? result)  {:result result}
        
        (seq result)       {:result result}
        
        :else              {:error "no result returned."}))))


#_(def ugly-str "keystore file:\nname:\nsolflare-keystore-64LxFA7pjU2CjSf3jgn6BpvEwsacMfez2RXrmDeaVYyN.json\ncontents:\n{\"publicKey\":\"64LxFA7pjU2CjSf3jgn6BpvEwsacMfez2RXrmDeaVYyN\",\"Crypto\":{\"cipher\":\"aes-128-ctr\",\"ciphertext\":\"bce33f3e8cd8cef8e764c0669b1a82aab2a34fc22f9ea4ba8c82eb976e83ca74389ecea4ba4fa9739557b1acf80aab93df59deb407f30eb675eca4f81f2c4bda\",\"cipherparams\":{\"counter\":601479},\"kdf\":\"pbkdf2\",\"kdfparams\":{\"c\":8192,\"dklen\":32,\"prf\":\"sha512\",\"salt\":\"ef021e15dd2cb8737a26fede21afa80867659cedb315bf6dfa126871d6298d2a\"}}}")

;; Better pretty printing of multi-line strings.
(defn- to-group [s]
  (into []
        (cons :group
              (cons [:line]
                    (map (fn [l] [:nest 2 l :break])
                            (clojure.string/split-lines s))))))


(extend EdnPrinter
  fipp.visit/IVisitor
  {:visit-string (fn [this x]
                   (binding [*print-readably* true]
                     (if (clojure.string/includes? x "\n")
                       (to-group x)
                       [:text (pr-str x)])))})


(defn- pp
  "Pretty prints out."
  [stuff]
  (fipp/pprint stuff))


(defn- printout [{:keys [error result]}]
  (if error
    (println error)
    (cond
      (string? result)   (println result)
      (= :edn @fmt)      (pp result)
      (= :json @fmt)     (println (generate-string result))
      (= :table @fmt)    (t/table result))))


(defn- console []
  (loop []
    (print "pass> ") (flush)
    (let [input (read-line)
          cmd (read-cmd input)]
      (if (or (= input ":quit") (= input "quit") (= input ":q") (= input "q"))
        (do (print "\033[H\033[2J") (flush) (println "bye!") (flush))
        (let [{:keys [error result]} cmd
              outcome (if error
                        {:error error}
                        (apply exec-cmd result))]
          (printout outcome)
          (flush)
          (recur))))))


;; --------------------------------------
;; ----------- Initialization -----------
;; --------------------------------------

(def miscalled
  (str
   "That wasn't quite right! Here's the help:\n\n"
   app-help))


(defn password-input []
  (println "Enter main password:")
  (String. (.readPassword (System/console))))


(defn- init
  [& args]
  (if-let [db (first args)]
    (do
        (println
         (str
          (if (fs/exists? db) "Authenticate against: " "Initialize new db: ")
          db))
        (let [pwd (password-input)
              {:keys [error result]} (do-cmd :init db pwd)]
          (if (not result)
            (do
              (println "Failed to authenticate")
              (System/exit 0))
            (do
              (reset! default-parent (read-str (fetch-config "default-parent")))
              (console)))))
    
    (if-let [db (System/getend "PASS_DB_PATH")]
      (init db)
      (println miscalled))))


(def cli-options
  [["-h" "--help"]])


(defn -main [& args]
  (let [{:keys [errors arguments options]} (parse-opts args cli-options)
        arguments (map read-str arguments)]
    (cond
      errors              (println (first errors))

      (:help options)     (println app-help)

      :else
      (if (seq arguments)
        (apply init arguments)
        (println "Please specify a database file.")))))
