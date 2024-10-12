(defproject trading-system-test-suite "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 ;; docjure
                 [dk.ative/docjure "1.19.0"]
                 ]
  :repositories [["clojars" {:url "https://repo.clojars.org/"}]]
  :mirrors {"clojars" {:name "mirror"
                       :url "https://mirrors.tuna.tsinghua.edu.cn/clojars/"}}
  :repl-options {:init-ns trading-system-test-suite.core})
