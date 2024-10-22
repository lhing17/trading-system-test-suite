(ns trading-system-test-suite.rule
  (:require [trading-system-test-suite.xls :as xls]
            [clojure.java.io :as jio]))


(def cash (atom 100000))

;; 初始持仓状态——空仓 0-空仓 1-半仓 2-满仓
(def position (atom 0))

;; 初始突破状态——未突破
(def breakout (atom [:none :none]))

;; 买入价格
(def buy-price (atom 0))


(defn reset-state! []
  (reset! cash 100000)
  (reset! position 0)
  (reset! breakout [:none :none])
  (reset! buy-price 0))

(defn check-state []
  {:cash      @cash
   :position  @position
   :breakout  @breakout
   :buy-price @buy-price})


(defprotocol Rule
  (match? [this daily-data index]
    "判断某条数据是否符合规则")
  (set-state! [this daily-data index]
    "设置状态"))



(defrecord BreakoutRule [item direction price-type]
  Rule
  (match? [_ daily-data index]
    (if (<= index 1)
      false
      (let [current-daily (nth daily-data (dec index))
            previous-daily (nth daily-data (- index 2))
            current-close (get current-daily price-type)
            previous-close (get previous-daily price-type)]
        (cond
          (nil? (get current-daily item)) false
          (nil? (get previous-daily item)) false
          (= direction :up) (and (> current-close (get current-daily item))
                                 (< previous-close (get previous-daily item)))
          (= direction :down) (and (< current-close (get current-daily item))
                                   (> previous-close (get previous-daily item)))
          :else false))))
  (set-state! [this daily-data index]
    (cond ((complement match?) this daily-data index) nil
          (= direction :up) (reset! breakout [:above (get daily-data index item)])
          (= direction :down) (reset! breakout [:below (get daily-data index item)]))))

(defrecord PositionRule [pos]
  Rule
  (match? [_ _ _]
    (= pos @position))
  (set-state! [_ _ _]
    nil))


(defrecord StateRule [state]
  Rule
  (match? [_ _ _]
    (and (= (first state) (first @breakout))
         (= (second state) (second @breakout))))
  (set-state! [_ _ _]
    nil))


;; 止损规则
(defrecord StopLossRule [atr-times]
  Rule
  (match? [_ daily-data index]
    (let [current-daily (nth daily-data (dec index))
          current-close (:close current-daily)
          atr (:atr-moving-average current-daily)]
      (cond (< current-close (- @buy-price (* atr atr-times))) true
            :else false)))

  (set-state! [_ _ _]
    nil))

(def rules
  {
   :breakout-above-bollinger-upper         (->BreakoutRule :bollinger-upper :up :close)
   :breakout-below-bollinger-lower         (->BreakoutRule :bollinger-lower :down :low)
   :breakout-below-thinner-bollinger-upper (->BreakoutRule :thinner-bollinger-upper :down :close)
   :breakout-above-thinner-bollinger-lower (->BreakoutRule :thinner-bollinger-lower :up :close)
   :breakout-above-moving-average          (->BreakoutRule :moving-average :up :close)
   :breakout-below-moving-average          (->BreakoutRule :moving-average :down :close)
   :position-empty                         (->PositionRule 0)
   :position-half                          (->PositionRule 1)
   :position-full                          (->PositionRule 2)
   :state-above-bollinger-upper            (->StateRule [:above :bollinger-upper])
   :state-below-bollinger-lower            (->StateRule [:below :bollinger-lower])
   :state-below-thinner-bollinger-upper    (->StateRule [:below :thinner-bollinger-upper])
   :state-above-thinner-bollinger-lower    (->StateRule [:above :thinner-bollinger-lower])
   :state-above-moving-average             (->StateRule [:above :moving-average])
   :state-below-moving-average             (->StateRule [:below :moving-average])
   :stop-loss-2atr                         (->StopLossRule 2)
   })


(defn can-buy-or-sell? [daily-data index ruleset]
  (if (coll? (first ruleset))
    ;; 如果ruleset是个列表的列表，表示有多个或关系的规则集，只要有一个规则集满足即可
    (some #(can-buy-or-sell? daily-data index %) ruleset)
    ;; ruleset是个keyword的列表，表示有多个且关系的规则集，所有规则都要满足
    (every? (fn [rule] (match? rule daily-data index))
            (map #(% rules) ruleset))))


(defn set-state-after-buy! [daily-data index ruleset]
  (doseq [rule (flatten ruleset)]
    (set-state! (get rules rule) daily-data index))
  (reset! buy-price (:close (nth daily-data (dec index))))
  (reset! position 2))

(defn set-state-after-sell! [daily-data index ruleset]
  (doseq [rule (flatten ruleset)]
    (set-state! (get rules rule) daily-data index))
  (reset! position 0))

(defn set-state-after-nothing! [daily-data index buy-ruleset sell-ruleset]
  (doseq [rule (flatten buy-ruleset)]
    (set-state! (get rules rule) daily-data index))
  (doseq [rule (flatten sell-ruleset)]
    (set-state! (get rules rule) daily-data index)))


(defn run! [daily-data buy-ruleset sell-ruleset]
  (loop [index 1 operations []]
    (if (> index (count daily-data))
      operations
      (cond
        (can-buy-or-sell? daily-data index buy-ruleset)
        (do
          (set-state-after-buy! daily-data index buy-ruleset)
          (recur (inc index) (conj operations [:buy index])))
        (can-buy-or-sell? daily-data index sell-ruleset)
        (do
          (set-state-after-sell! daily-data index sell-ruleset)
          (recur (inc index) (conj operations [:sell index])))
        :else
        (do
          (set-state-after-nothing! daily-data index buy-ruleset sell-ruleset)
          (recur (inc index) operations))))))

(defn modify-trades [daily-data trades]
  (if (= :buy (first (last trades)))
    (conj trades [:sell (count daily-data)])
    trades))

(defn walk-through-trades [daily-data trades]
  (loop [trades (modify-trades daily-data trades) trade-number 0.0 ret []]
    (if (empty? trades)
      (do (println "交易结束") ret)
      (let [trade (first trades)
            trade-type (first trade)
            index (second trade)
            daily (nth daily-data (dec index))]
          (if (= trade-type :buy)
            (let [num (/ @cash (:close daily))]
              (println "交易日期：" (:date daily)
                       "买入价格：" (:close daily)
                       "买入数量：" num)
              (reset! cash 0)
              (recur (rest trades) num ret))
            (let [amount (* trade-number (:close daily))]
              (println "交易日期：" (:date daily)
                       "卖出价格：" (:close daily)
                       "卖出数量：" amount)
              (reset! cash amount)
              (recur (rest trades) 0 (conj ret {:date (:date daily) :amount amount}))))))))


(comment
  (def daily-data (xls/read-daily-data (jio/file "/Users/lianghao/Documents/交易日志/沪深300指数历史数据.xlsx")))
  (count daily-data)
  (reset-state!)
  (def trades (run! (take 672 daily-data)
                    [[:breakout-above-bollinger-upper :position-empty]
                     [:breakout-above-thinner-bollinger-lower :position-empty :state-below-thinner-bollinger-upper]]
                    [[:breakout-below-thinner-bollinger-upper :position-full :state-above-bollinger-upper]
                     [:stop-loss-2atr :position-full]]))
  (def ret (walk-through-trades (take 672 daily-data) trades))
  (def sdf (java.text.SimpleDateFormat. "yyyy-MM-dd"))
  (doseq [x ret] (println (.format sdf (:date x)) (:amount x)))
  (check-state)

  )



