(ns pass.docs
  (:require [clojure.string :as str]))


(defn- pretty-doc
  [s]
  (let [ss (str/split s #"\n")]
    (apply str
           (interpose "\n"
                      (map str/trim ss)))))


(defn- pretty-name [n] (str (keyword (clojure.string/replace n #"!" ""))))


(defn- pretty-arglists [al] (str "arglists:  " al))


(defn- extras [exs]
  (when exs
    (str (apply str (interpose "\n" exs)) "\n")))


(defn docstring
  "Returns a docstring for the var."
  [cmd & {:keys [extra-msgs]}]
  (let [m (meta cmd)
        {:keys [after-name after-arglists after-doc]} extra-msgs]
    (str (pretty-name (:name m)) "\n"
         (extras after-name)
         "-------------------------\n"
         (pretty-arglists (:arglists m)) "\n"
         (extras after-arglists) "\n"
         (pretty-doc (:doc m)) "\n"
         (extras after-doc))))


;; The pass application's docs for locally defined commands.
(defn local-docs
  "Returns a map of the local application's cmds and their docstrings."
  ([] (local-docs nil))
  ([state]
   (-> {}
       (assoc :set-default-parent
              (str ":set-default-parent\n"
                   "-------------------------\n"
                   "arglists: ([parent])\n\n"
                   "Sets a default parent for all operations that would otherwise need a parent\n"
                   "to be specified. Useful, if you want to arrange your database as a flat list of keys.\n"
                   "To clear a previously set default parent, pass nil into this function."
                   (when (:def-parent state) (str "\n* default parent currently set to " (:def-parent state)))))
       (assoc :get-default-parent
              (str ":get-default-parent\n"
                   "-------------------------\n"
                   "arglists: ([])\n\n"
                   "Returns the current value of the default parent, or nil if it is not set."
                   (when (:def-parent state) (str "\n* default parent currently set to " (:def-parent state)))))
       (assoc :help
              (str ":help\n"
                   "-------------------------\n"
                   "arglists: ([][cmd])\n\n"
                   "Get general help or help on a particular :cmd"))
       (assoc :fmt
              (str ":fmt\n"
                   "-------------------------\n"
                   "arglists: ([format])\n\n"
                   "Sets the output format. Possible options are:\n"
                   ":edn (the default), :json and :table\n"
                   "* The current value is " (:fmt state)))
       (assoc :import
              (str ":import\n"
                   "-------------------------\n"
                   "arglists: ([file & {:keys [format], :or {format :json}}])\n\n"
                   "Imports nodes from a file into the database. nodes should be\n"
                   "a sequence of maps, where each map has a :key, a :value and\n"
                   "optionally a :parent.\n"
                   "the :format argument can be either :json (default) or :edn."))
       (assoc :export
              (str ":export\n"
                   "-------------------------\n"
                   "arglists: ([file & {:keys [format], :or {format :json}}])\n\n"
                   "Exports to file all nodes in the database in either :json or :edn format."))
       (assoc :gen
              (str ":gen\n"
                   "-------------------------\n"
                   "arglists: ([][length])\n\n"
                   "Generates a random password of length."))
       (assoc :syntax
              (str "Help on the correct syntax for pass commands\n"
                   "--------------------------------------------\n\n"
                   "1. arglists\n\n"
                   "The help for a command has an `arglists` line. For example the :gen function"
                   " has this arglists:\n\n"
                   " arglists: ([][length])\n\n"
                   "The () indicates choice; there are two ways to call this function.\n"
                   "[] means without any arguments, and [length] means with a length parameter.\n\n"
                   "The :get-node function has this arglists:\n\n"
                   " arglists: [parent k & {:keys [decrypt?] :or {decrypt? true}}]\n\n"
                   "There's no (), so only one way to call this function; with parameters `parent`\n"
                   "and `key` and optionally a `keyword argument` to tell the function whether to\n"
                   "decrypt the returned node or not. Valid examples of calling this function:\n\n"
                   "  :get-node :root facebook.com  (take the default value for :decrypt?)\n"
                   "  :get-node :root facebook.com :decrypt? false\n\n"
                   "Both are valid.\n\n\n"
                   "2. String handling\n\n"
                   "A string containing any of the characters:\n"
                   " space\n"
                   " `\n"
                   " #\n"
                   " ~\n"
                   " @\n"
                   " [\n"
                   " ]\n"
                   " (\n"
                   " )\n"
                   " {\n"
                   " }\n"
                   "must be double quoted.\n"
                   " e.g. \"Iam@a]{(pass`W0rd~\"\n"
                   " e.g. \"I am fine!\"\n"
                   " e.g. facebook.com    - Ok not to double quote, as no spaces or special characters."))
       (assoc :update-node-with
              (str ":update-node-with\n"
                   "-------------------------\n"
                   "arglists: ([key f & args][parent key f & args])\n\n"
                   "Updates the node specified by parent and k's value by applying the function\n"
                   "f to the old value and arg(uments).\n"
                   "Available functions to use for f are:\n"
                   "`assoc`, `dissoc` for adding and removing keys from a map.\n"
                   "`update`, `update-in` for updating values inside, deep inside a map.\n"
                   "`str` for adding to an existing string.\n"
                   "`add-line` for adding a new line to an existing string.\n"
                   "`merge` for merging two maps.\n"
                   "`conj` for adding items to a list or vector respectively.\n"
                   "Please see Clojure's help online for how to use each of these functions."))
       (assoc :quit
              (str ":quit\n"
                   "-------------------------\n"
                   "Quits the Pass console.\n"
                   "You can also type 'q' ':q' or 'quit' followed by <enter>."))
       (assoc :copy-in-node
              (str ":copy-in-node\n"
                   "-------------------------\n"
                   "arglists:  ([key keys] [parent key keys])\n"
                   "like `:get-in-node` but copies the result to the clipboard.")))))
