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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static io.nuls.contract.sdk.Utils.emit;
import static io.nuls.contract.sdk.Utils.require;
import static io.nuls.contract.util.PocmUtil.toNa;

/**
 * @author: PierreLuo
 * @date: 2019-03-07
 */
public class Pocm extends SimpleToken {

    // 合约创建高度
    private final long createHeight;
    // 价格因子，每抵押XX个NULS，一个奖励发放周期发放一个新token, price = 1/priceSeed
    private BigInteger priceSeed;
    // 奖励发放周期（参数类型为数字，每过XXXX块发放一次）
    private int awardingCycle;
    // 奖励减半周期（可选参数，若选择，则参数类型为数字，每XXXXX块奖励减半）
    private int rewardHalvingCycle;
    // 最低抵押NULS数量
    private BigInteger minimumDeposit;
    // 最短锁定区块（参数类型为数字，XXXXX块后才可退出抵押）
    private int minimumLocked;
    // 最大抵押地址数量（可选参数）
    private int maximumDepositAddressCount;

    // 用户抵押信息
    private Map<String, MiningInfo> users = new HashMap<String, MiningInfo>();

    // 总抵押金额
    private BigInteger totalDeposit;
    // 总抵押地址数量
    private int totalDepositAddressCount;


    public Pocm(String name, String symbol, BigInteger initialAmount, int decimals,
                BigDecimal priceSeedNULS, int awardingCycle, int rewardHalvingCycle,
                BigDecimal minimumDepositNULS, int minimumLocked, int maximumDepositAddressCount) {
        super(name, symbol, initialAmount, decimals);
        this.createHeight = Block.number();
        this.totalDeposit = BigInteger.ZERO;
        this.totalDepositAddressCount = 0;
        this.priceSeed = toNa(priceSeedNULS);
        this.awardingCycle = awardingCycle;
        this.rewardHalvingCycle = rewardHalvingCycle;
        this.minimumDeposit = toNa(minimumDepositNULS);
        this.minimumLocked = minimumLocked;
        this.maximumDepositAddressCount = maximumDepositAddressCount;
    }

    /**
     *  抵押
     */
    @Payable
    public MiningInfo deposit() {
        require(totalDepositAddressCount + 1 <= maximumDepositAddressCount, "超过最大抵押地址数量");
        Address user = Msg.sender();
        String userStr = user.toString();
        require(!users.containsKey(userStr), "不可重复抵押");
        BigInteger value = Msg.value();
        require(value.compareTo(minimumDeposit) > 0, "未达到最低抵押值");
        MiningInfo info = new MiningInfo();
        info.setDepositAmount(value);
        info.setDepositHeight(Block.number());
        users.put(userStr, info);
        totalDeposit = totalDeposit.add(value);
        totalDepositAddressCount += 1;
        return info;
    }

    /**
     *  追加抵押
     */
    @Payable
    public MiningInfo increaseDeposit() {
        MiningInfo info = receive();
        BigInteger value = Msg.value();
        info.setDepositAmount(info.getDepositAmount().add(value));
        totalDeposit = totalDeposit.add(value);
        return info;
    }

    /**
     *  退出
     */
    public MiningInfo quit() {
        Address user = Msg.sender();
        MiningInfo miningInfo = getMiningInfo(user);

        long unLockedHeight = checkLocked(miningInfo);
        require(unLockedHeight != -1, "挖矿锁定中, 解锁高度是 " + unLockedHeight);
        // 发放奖励
        this.receive(user, miningInfo);

        // 退押金
        BigInteger deposit = miningInfo.getDepositAmount();
        totalDeposit = totalDeposit.subtract(deposit);
        totalDepositAddressCount -= 1;
        //TODO pierre 退出后是否保留该账户的挖矿记录
        Msg.sender().transfer(deposit);
        return miningInfo;
    }

    /**
     *  领取奖励
     */
    public MiningInfo receive() {
        Address user = Msg.sender();
        MiningInfo miningInfo = getMiningInfo(user);
        this.receive(user, miningInfo);
        return miningInfo;
    }

    private long checkLocked(MiningInfo miningInfo) {
        long currentHeight = Block.number();
        long depositHeight = miningInfo.getDepositHeight();
        long unLockedHeight = depositHeight + minimumLocked + 1;
        if(unLockedHeight > currentHeight) {
            return unLockedHeight;
        }
        return -1;
    }

    private MiningInfo getMiningInfo(Address user) {
        String userStr = user.toString();
        MiningInfo miningInfo = users.get(userStr);
        require(miningInfo != null, "此用户未参与");
        return miningInfo;
    }

    private void receive(Address user, MiningInfo miningInfo) {

        // 奖励计算, 计算每次挖矿的高度是否已达到奖励减半周期的范围，若达到，则当次奖励减半，以此类推
        BigInteger thisMining = this.calcMining(miningInfo);

        miningInfo.setTotalMining(miningInfo.getTotalMining().add(thisMining));
        miningInfo.setReceivedMining(miningInfo.getReceivedMining().add(thisMining));

        this.setTotalSupply(totalSupply().add(thisMining));

        addBalance(user, thisMining);
        emit(new TransferEvent(null, user, thisMining));
    }

    private BigInteger calcMining(MiningInfo miningInfo) {
        BigInteger mining = BigInteger.ZERO;

        long currentHeight = Block.number();
        long nextMiningHeight = miningInfo.getNextMiningHeight();
        long depositHeight = miningInfo.getDepositHeight();
        BigInteger depositAmount = miningInfo.getDepositAmount();
        int miningCount = miningInfo.getMiningCount();
        if(nextMiningHeight == 0) {
            nextMiningHeight = depositHeight + awardingCycle + 1;
        }
        BigInteger currentPriceSeed = this.priceSeed;
        int i = 0;
        while (nextMiningHeight <= currentHeight) {
            i++;
            currentPriceSeed = calcPriceSeed(nextMiningHeight);
            mining = mining.add(depositAmount.divide(currentPriceSeed));
            nextMiningHeight += awardingCycle + 1;
        }
        miningInfo.setMiningCount(miningCount + i);
        miningInfo.setNextMiningHeight(nextMiningHeight);
        if(mining.compareTo(BigInteger.ZERO) > 0) {
            mining = mining.pow(this.decimals());
        }
        return mining;
    }

    private BigInteger calcPriceSeed(long nextMiningHeight) {
        BigInteger d = BigInteger.valueOf(2L);
        BigInteger currentPriceSeed = this.priceSeed;
        long triggerH = this.createHeight + this.rewardHalvingCycle + 1;
        while(triggerH <= nextMiningHeight) {
            currentPriceSeed = currentPriceSeed.multiply(d);
            triggerH += this.rewardHalvingCycle + 1;
        }
        return currentPriceSeed;
    }
}
