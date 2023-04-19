(ns pass.command
  "Command Parsing"
  (:require [clojure.walk :refer [postwalk]]
            [clojure.edn :refer [read-string]]
            [clojure.string :refer [includes? replace]]))

;; ISSUE
;; When a collection contains a string that can't be read by edn/read-string
;; the whole collection won't be read - will be left as just a string
;; process:  tokenize -> map read-str
;; I need a better tokenizer!
;; how it works:
;; Collections themselves have to be tokenized and any strings tagged, then re-assembled
;; tag could be something like pass.command/string
;; Then in read-str, all strings are now tagged and I can write my own string reader

;; a convenience function for adding a line
(defn add-line [s new-line]
  (str s "\n" new-line))


(defn- retain-symbol [form]
  (if (contains? #{'assoc 'dissoc 'update 'update-in 'str 'conj 'merge 'add-line} form)
    form
    (str form)))


(defn- shorter? [s1 s2]
  (< (count (str s1)) (count s2)))


(def ^:private readers
  {'string (fn [s] (str s))})


(def ^:private read-err
  (ex-info
   (str "The command couldn't be read. Did you need to double quote any strings?\n"
        "Strings containing spaces or any of the characters: # ` @ ~ [ ] { } ( )\n"
        "must be surrounded by double quotes.")
   {}))


(defn read-str
  "Reads the string as edn. Converts any symbols to strings with a postwalk."
  [s & {:keys [retain?] :or {retain? false}}]
  (try
    (let [form (read-string s)]
      (cond
        ;; work around Clojure special characters truncating read-str
        ;; e.g. { } [ } ( ) ' #
        (or (and (or (symbol? form) (string? form))
                 (shorter? form (replace s #"\"" "")))
            (and (map? form) (some #(not (or (keyword? %) (string? %))) (keys form))))
        (throw read-err)
        
        :else
        (if form
          (postwalk
           (fn [form]
             (cond
               (symbol? form)  (if retain? (retain-symbol form) (str form))
               :else           form))
           form)
          nil)))
    (catch Exception e
      (if (string? s)
        s
        (throw read-err)))))


(def ^:private symbols-after
  {'assoc 0
   'dissoc 0
   'update 1
   'update-in 1
   'str 0
   'cons 0
   'conj 0
   'merge 0})


(defn- prune-syms
  "Works through seq of cmds, converting to string any that
   should not exist."
  [cmds]
  (let [allowed-count (atom 1)]
    (reduce
     (fn [acc cur]
       (if (symbol? cur)
         (let [c @allowed-count]
           (case c
             0     (conj acc (str cur))
             1     (do
                     (reset! allowed-count (get symbols-after cur))
                     (conj acc (resolve cur)))))
         (conj acc cur)))
     []
     cmds)))


(defn- update-last [f coll]
  (let [v (into [] coll)
        li (dec (count v))]
    (update v li f)))


(defn- concat-if-strs [coll]
  (if (every? string? coll)
    [(apply str (interpose " " coll))]
    coll))


(defn- tokenize
  "Splits s first on substrings and then on spc for non-substrings."
  [s]
  (let [chars (map char s)
        len (count s)
        in-str? (atom false)
        in-coll? (atom false)
        open-coll? (fn [ch] (or (= ch \[) (= ch \() (= ch \{)))
        close-coll? (fn [ch] (or (= ch \]) (= ch \)) (= ch \})))]
    (map
     (fn [char-array] (apply str char-array))
     (reduce
      (fn [acc cur]
        (let [last-index (dec (count acc))]
          (cond
            (= cur \")
            (if @in-str?
              (do (reset! in-str? false) (update acc last-index conj cur))
              (do (reset! in-str? true)
                  (update acc last-index conj cur)))

            @in-str?
            (update acc last-index conj cur)

            (open-coll? cur)
            (do (reset! in-coll? true)
                (update acc last-index conj cur))

            (close-coll? cur)
            (do (reset! in-coll? false)
                (update acc last-index conj cur))

            @in-coll?
            (update acc last-index conj cur)

            (= cur \space)
            (conj acc [])

            :else
            (update acc last-index conj cur))))
      [[]]
      s))))


(def ^:private parsers
  ;; We use mapv (rather than map) to force immediate evaluation and therefore allow
  ;; any errors thrown to be caught in read-cmd's try catch block.
  {:simple (fn [tokens] (->> tokens
                             (mapv (fn [token] (read-str token :retain? false)))
                             (remove nil?)))
   
   :complex (fn [tokens] (->> tokens
                              (mapv (fn [token] (read-str token :retain? true)))
                              prune-syms
                              (remove nil?)))})


(def ^:private complex-cmds #{":update-node-with"})


(defn read-cmd
  [s]
  (try
    (let [tokens (tokenize s)
          parser (if (contains? complex-cmds (first tokens))
                   (:complex parsers)
                   (:simple parsers))]
      {:result
       (parser tokens)})
    (catch Exception e
      {:error (str (.getMessage e) "\nSee also  :help :syntax")})))
