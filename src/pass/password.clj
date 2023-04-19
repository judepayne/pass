(ns pass.password)


(defn- remove-chars [range]
  (remove
   #(contains? #{96 92} %)
   range))


(defn generate-password
  ([] (generate-password 20))
  ([length]
   (let [available-chars (reduce (fn [acc val]
                                   (str acc (char val))) "" (remove-chars (range 33 123)))]
     (loop [password ""]
       (if (= (count password) length)
         password
         (recur (str password (rand-nth available-chars))))))))
