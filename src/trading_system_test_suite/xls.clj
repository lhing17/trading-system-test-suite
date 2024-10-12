(ns trading-system-test-suite.xls
  (:require [dk.ative.docjure.spreadsheet :refer :all]
            [trading-system-test-suite.daily :as daily]
            ))


(defn read-daily-data [file]
  (->> (load-workbook file)
       (select-columns {:A :date
                        :B :close
                        :C :open
                        :D :high
                        :E :low
                        :F :volume
                        :G :percent-change
                        })
       (drop 1)
       (mapv daily/map->Daily)
       (daily/update-daily)
       ))


(comment





  )
