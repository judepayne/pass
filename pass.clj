#!/usr/bin/env bb

;; wrapper script for pass.app to get rlwrap goodness. 

(ns pass
  (:require [babashka.process :refer [shell]]))


(defn- cur-dir
  "Returns the current directory" []
  (let [parts (clojure.string/split *file* #"/")]
    (clojure.string/join "/" (butlast parts))))


(let [cd (cur-dir)]
    (apply shell
           "rlwrap"
           "-pBlue"
           "-b" "'()=<>&+*|:;,\\'"
           "-f" (str cd "/.rlwrap-wordlist")
           "bb"
           (str cd "/pass.jar")
           *command-line-args*))
