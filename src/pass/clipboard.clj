(ns pass.clipboard
  (:require [babashka.process :refer [process]]))


(defn copy [text]
  (process "pbcopy" {:in text}))
