/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.contract.pocm;

import io.nuls.contract.model.MiningInfo;
import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Block;
import io.nuls.contract.sdk.BlockHeader;
import io.nuls.contract.sdk.Msg;
import io.nuls.contract.sdk.annotation.Payable;
import io.nuls.contract.token.SimpleToken;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static io.nuls.contract.sdk.Utils.emit;
import static io.nuls.contract.sdk.Utils.require;

/**
 * @author: PierreLuo
 * @date: 2019-03-07
 */
public class Pocm extends SimpleToken {

    //TODO pierre 兑换token比例

    // 初始挖矿奖励
    private BigInteger initialMiningAward;
    // 奖励发放周期（参数类型为数字，每过XXXX块发放一次）
    private int awardingCycle;
    // 奖励减半周期（可选参数，若选择，则参数类型为数字，每XXXXX块奖励减半）
    private int rewardHalvingCycle;
    // 最低抵押NULS数量
    private BigInteger minimumDeposit;
    // 最短锁定周期（参数类型为数字，每XXXXX块后才可退出抵押）
    private int minimumLockedCycle;
    // 最大抵押地址数量（可选参数）
    private int maximumDepositAddressCount;
    // 封顶挖出的token
    private BigInteger initialAmount;

    public Pocm(String name, String symbol, BigInteger initialAmount, int decimals) {
        super(name, symbol, BigInteger.ZERO, decimals);
        this.initialAmount = initialAmount;
    }

    private Map<String, MiningInfo> users = new HashMap<String, MiningInfo>();

    /**
     *  抵押
     */
    @Payable
    public boolean deposit() {
        BigInteger value = Msg.value();
        require(value.compareTo(minimumDeposit) > 0, "未达到最低抵押值");
        MiningInfo info = new MiningInfo();
        info.setDepositAmount(value);
        info.setDepositHeight(Block.currentBlockHeader().getHeight());
        users.put(Msg.sender().toString(), info);

        return false;
    }

    /**
     *  退出
     */
    public boolean withdraw() {

        return false;
    }

    /**
     *  挖矿
     */
    public boolean mint() {
        Address user = Msg.sender();
        String userStr = user.toString();
        MiningInfo miningInfo = users.get(userStr);
        //long depositHeight = miningInfo.getDepositHeight();
        //BlockHeader currentBlockHeader = Block.currentBlockHeader();
        //long currentHeight = currentBlockHeader.getHeight();
        int thisMiningCount = this.calcMiningCount(Block.currentBlockHeader().getHeight(), miningInfo.getDepositHeight(), miningInfo.getMiningCount());


        //TODO pierre 奖励发放

        //TODO pierre 奖励减半周期
        /**
         *  计算每次挖矿的高度是否已达到奖励减半周期的范围，若达到，则当次奖励减半，以此类推
         */
        BigInteger mint = initialMiningAward.multiply(BigInteger.valueOf(thisMiningCount));
        BigInteger totalSupply = getTotalSupply();
        require(totalSupply.add(mint).compareTo(initialAmount) <= 0, "超过封顶了");
        emit(new TransferEvent(this.owner, user, totalSupply));

        return false;
    }

    private int calcMiningCount(long currentHeight, long depositHeight, int currentMiningCount) {

        return 0;
    }
}
