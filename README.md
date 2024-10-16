# trading-system-test-suite

一个小型的测试套件，用于测试基于规则的市场交易体系。

## Usage

可以自己定义交易规则，然后基于历史数据测试交易体系的表现。
- daily.clj 每日交易数据，包括开盘价、收盘价、最高价、最低价、成交量等，还可以自定义一些指标。如移动平均值、标准差、布林带上下轨、ATR等。注意基础的交易数据是从excel文件中读取的，而指标是通过clojure代码计算的。
- xls.clj 用于读取excel文件中的数据，并将数据转换为daily.clj中的数据结构。注意读取后调用了计算指标的函数，所以在读取数据后，可以直接使用指标。
- rule.clj 交易规则，定义为protocol，可以自己扩展交易规则，目前实现了突破规则、头寸规则、状态规则和止损规则。

示例的交易策略为：
- 买入1：当价格突破布林带下轨，并回升至较窄的布林带下轨时，且空仓时买入。
- 买入2：当价格突破布林带上轨时，且空仓时买入。
- 卖出1：当价格突破布林带上轨，并回落至较窄的布林带上轨时，且持仓时卖出。
- 卖出2：止损卖出，止损点为买入价格-2*ATR。（2倍为配置项）

## License

Copyright © 2024 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
