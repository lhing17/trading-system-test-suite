(ns trading-system-test-suite.daily
  (:import (java.util Calendar Date)))

(defrecord Daily
  [
   ;; 交易日期
   ^Date date
   ;; 收盘价
   close
   ;; 开盘价
   open
   ;; 最高价
   high
   ;; 最低价
   low
   ;; 成交量
   volume
   ;; 涨跌幅
   percent-change
   ;; 移动平均值
   moving-average
   ;; 标准差
   deviation
   ;; 布林带上轨
   bollinger-upper
   ;; 布林带下轨
   bollinger-lower
   ;; 更窄的布林带上轨
   thinner-bollinger-upper
   ;; 更窄的布林带下轨
   thinner-bollinger-lower
   ;; 是否超过布林带上轨
   above-bollinger-upper
   ;; 是否低于布林带下轨
   below-bollinger-lower
   ;; 是否超过更窄的布林带上轨
   above-thinner-bollinger-upper
   ;; 是否低于更窄的布林带下轨
   below-thinner-bollinger-lower
   ;; ATR (Average True Range)
   atr
   ;; ATR (Average True Range) 的移动平均值-20天
   atr-moving-average
   ])


(def config {
             ;; 移动平均值的周期
             :moving-average-period       60
             ;; 布林带的标准差倍数
             :bollinger-deviation         2
             ;; 更窄的布林带的标准差倍数
             :thinner-bollinger-deviation 1.6
             })

(defn- make-date [year month day]
  (let [c (Calendar/getInstance)]
    (.set c year (inc month) day)
    (.getTime c)))

(defn- calculate-moving-average [daily-data index]
  ;; 计算移动平均值，index 为当前日期在 daily-data 中的索引，从1开始
  (if (or (< index (:moving-average-period config)) (> index (count daily-data)))
    :NA
    (let [sum (reduce + (map :close (subvec daily-data (- index (:moving-average-period config)) index)))]
      (* 1.0 (/ sum (:moving-average-period config))))))

(defn- calculate-deviation [daily-data index]
  ;; 计算标准差，index 为当前日期在 daily-data 中的索引，从1开始
  (if (or (< index (:moving-average-period config)) (> index (count daily-data)))
    :NA
    (let [average (calculate-moving-average daily-data index)
          sum (reduce + (map
                          (fn [x] (let [diff (- (:close x) average)] (* diff diff)))
                          (subvec daily-data (- index (:moving-average-period config)) index)))]

      (Math/sqrt (/ sum (:moving-average-period config))))))

(defn- calculate-bollinger [daily-data index coefficient]
  ;; 计算布林带，index 为当前日期在 daily-data 中的索引，从1开始
  (if (or (< index (:moving-average-period config)) (> index (count daily-data)))
    :NA
    (let [average (calculate-moving-average daily-data index)
          deviation (calculate-deviation daily-data index)]
      {:bollinger-upper (+ average (* deviation coefficient))
       :bollinger-lower (- average (* deviation coefficient))})))

(defn- calculate-atr [daily-data index]
  (cond (= index 1) (- (:high (first daily-data)) (:low (first daily-data)))
        (> index (count daily-data)) :NA
        :else (max (- (:high (nth daily-data (dec index))) (:low (nth daily-data (dec index))))
                   (- (:high (nth daily-data (dec index))) (:close (nth daily-data (- index 2))))
                   (- (:close (nth daily-data (- index 2))) (:low (nth daily-data (dec index)))))

        ))

(def calculate-atr-moving-average
  (memoize (fn [daily-data index]
             ;; 计算 ATR (Average True Range)，index 为当前日期在 daily-data 中的索引，从1开始
             (let [true-range (calculate-atr daily-data index)]
               (cond (or (< index 20) (> index (count daily-data))) true-range
                     (= index 20) (/ (reduce + (map (fn [x] (calculate-atr daily-data x)) (range 1 21))) 20)
                     :else (/
                             (+
                               (* 19
                                  (calculate-atr-moving-average daily-data (dec index)))
                               true-range)
                             20))))))

(defn update-daily [daily-data]
  (map-indexed
    (fn [index daily]
      (let [bollinger (calculate-bollinger daily-data (inc index) (:bollinger-deviation config))
            thinner-bollinger (calculate-bollinger daily-data (inc index) (:thinner-bollinger-deviation config))]
        (assoc
          (assoc daily
            :moving-average (calculate-moving-average daily-data (inc index))
            :deviation (calculate-deviation daily-data (inc index))
            :bollinger-upper (:bollinger-upper bollinger)
            :bollinger-lower (:bollinger-lower bollinger)
            :thinner-bollinger-upper (:bollinger-upper thinner-bollinger)
            :thinner-bollinger-lower (:bollinger-lower thinner-bollinger)
            :atr (calculate-atr daily-data (inc index))
            :atr-moving-average (calculate-atr-moving-average daily-data (inc index)))
          :above-bollinger-upper (if (:bollinger-upper bollinger) (> (:close daily) (:bollinger-upper bollinger)) :NA)
          :below-bollinger-lower (if (:bollinger-lower bollinger) (< (:close daily) (:bollinger-lower bollinger)) :NA)
          :above-thinner-bollinger-upper (if (:bollinger-upper thinner-bollinger) (> (:close daily) (:bollinger-upper thinner-bollinger)) :NA)
          :below-thinner-bollinger-lower (if (:bollinger-lower thinner-bollinger) (< (:close daily) (:bollinger-lower thinner-bollinger)) :NA)
          )))
    daily-data))




(comment
  (calculate-moving-average daily-data 1)
  (calculate-moving-average daily-data 19)
  (calculate-deviation daily-data 22)
  (calculate-moving-average daily-data 30)
  (calculate-moving-average daily-data 31)
  (calculate-moving-average daily-data 40)

  (calculate-bollinger daily-data 30 1.6)

  (calculate-atr daily-data 20)
  (map (fn [x] (calculate-atr daily-data x)) (range 1 21))
  (/ (reduce + (map (fn [x] (calculate-atr daily-data x)) (range 1 21))) 20)
  (update-daily daily-data)
  )

