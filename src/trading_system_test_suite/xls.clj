(ns trading-system-test-suite.xls
  (:require [clojure.java.io :as jio]
            [dk.ative.docjure.spreadsheet :refer :all]
            [trading-system-test-suite.daily :as daily]
            ))


(defn read-daily-data [file]
  (->> (load-workbook file)
       (select-sheet "daily")
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
  (def daily-data (read-daily-data (jio/file "/Users/lianghao/Documents/交易日志/沪深300指数历史数据.xlsx")))





  )
