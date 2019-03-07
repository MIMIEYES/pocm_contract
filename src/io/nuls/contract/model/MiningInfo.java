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
package io.nuls.contract.model;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 * @date: 2019-03-07
 */
public class MiningInfo {
    private BigInteger depositAmount;
    private long depositHeight;
    private BigInteger totalMining;
    private int miningCount;

    public MiningInfo() {
        this.totalMining = BigInteger.ZERO;
        this.miningCount = 0;
    }

    public BigInteger getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigInteger depositAmount) {
        this.depositAmount = depositAmount;
    }

    public long getDepositHeight() {
        return depositHeight;
    }

    public void setDepositHeight(long depositHeight) {
        this.depositHeight = depositHeight;
    }

    public BigInteger getTotalMining() {
        return totalMining;
    }

    public void setTotalMining(BigInteger totalMining) {
        this.totalMining = totalMining;
    }

    public int getMiningCount() {
        return miningCount;
    }

    public void setMiningCount(int miningCount) {
        this.miningCount = miningCount;
    }
}
