/** DDIVU.java
 *
 * Instruction DDIVU of the MIPS64 Instruction Set
 * (c) 2006 EduMips64 project - Andrea Milazzo (Mancausoft)
 *
 * This file is part of the EduMIPS64 project, and is released under the GNU
 * General Public License.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


package org.edumips64.core.is;
import org.edumips64.core.*;
import org.edumips64.utils.*;
import java.math.BigInteger;

//per diagnostica
import java.util.*;

/**
 * <pre>
 *      Syntax: DDIVU rs, rt
 * Description: (LO, HI) = rs / rt
 *              To divide 64-bit unsigned integers
 *  *           The 64-bit doubleword in GPR rs is divided by the 64-bit
 *              doubleword in GPR rt, treating both operands as signed values.
 *              The 64-bit quotient is placed into special register LO and the
 *              64-bit remainder is placed into special register HI.
 *              No arithmetic exception occurs under any circumstances.
 *</pre>
 * @author Trubia Massimo, Russo Daniele
 */
class DDIVU extends ALU_RType {
  final static int RS_FIELD = 0;
  final static int RT_FIELD = 1;
  final static int LO_REG = 2;
  final static int HI_REG = 3;
  final String OPCODE_VALUE = "011110";

  public DDIVU() {
    super.OPCODE_VALUE = OPCODE_VALUE;
    syntax = "%R,%R";
    name = "DDIVU";
  }
  public void ID() throws RAWException, IrregularWriteOperationException, IrregularStringOfBitsException {
    //if source registers are valid passing their own values into temporary registers
    Register rs = cpu.getRegister(params.get(RS_FIELD));
    Register rt = cpu.getRegister(params.get(RT_FIELD));

    if (rs.getWriteSemaphore() > 0 || rt.getWriteSemaphore() > 0) {
      throw new RAWException();
    }

    TR[RS_FIELD] = rs;
    TR[RT_FIELD] = rt;
    //locking the destination registers (quotient and remainder)
    cpu.getLO().incrWriteSemaphore();
    cpu.getHI().incrWriteSemaphore();

  }
  public void EX() throws IrregularStringOfBitsException, IntegerOverflowException, TwosComplementSumException, DivisionByZeroException {

    //getting values from temporary registers
    BigInteger rs = new BigInteger(TR[RS_FIELD].getHexString(), 16);
    BigInteger rt = new BigInteger(TR[RT_FIELD].getHexString(), 16);

    //performing operations
    BigInteger result[] = null;

    try {
      result = rs.divideAndRemainder(rt);
    } catch (ArithmeticException e) {
      if (isEnableForwarding()) {
        cpu.getLO().decrWriteSemaphore();
        cpu.getHI().decrWriteSemaphore();
      }

      throw new DivisionByZeroException();
    }

    //writing result in temporary registers
    String tmp = result[0].toString(2);  //quotient

    while (tmp.length() < 64) {
      tmp = "0" + tmp;
    }

    TR[LO_REG].setBits(tmp, 0);

    tmp = result[1].toString(2);  //reminder

    while (tmp.length() < 64) {
      tmp = "0" + tmp;
    }

    TR[HI_REG].setBits(tmp, 0);

    if (isEnableForwarding()) {
      doWB();
    }
  }

  public void WB() throws IrregularStringOfBitsException {
    if (!isEnableForwarding()) {
      doWB();
    }
  }
  public void doWB() throws IrregularStringOfBitsException {
    //passing results from temporary registers to destination registers and unlocking them
    Register lo = cpu.getLO();
    Register hi = cpu.getHI();
    lo.setBits(TR[LO_REG].getBinString(), 0);
    hi.setBits(TR[HI_REG].getBinString(), 0);
    lo.decrWriteSemaphore();
    hi.decrWriteSemaphore();
  }
  public void pack() throws IrregularStringOfBitsException {
    //conversion of instruction parameters of "params" list to the "repr" form (32 binary value)
    repr.setBits(OPCODE_VALUE, OPCODE_VALUE_INIT);
    repr.setBits(Converter.intToBin(RS_FIELD_LENGTH, params.get(RS_FIELD)), RS_FIELD_INIT);
    repr.setBits(Converter.intToBin(RT_FIELD_LENGTH, params.get(RT_FIELD)), RT_FIELD_INIT);
  }



  public static void main(String[] args) {
    DDIV ins = new DDIV();
    List<Integer>params = new Vector<Integer>();
    int rs = 1;
    int rt = 2;
    params.add(rs);  //dividendo
    params.add(rt);  //divisore

    try {
      cpu.getRegister(rs).writeDoubleWord(-9223372036854775807L);   //rs register
      cpu.getRegister(rt).writeDoubleWord(922);     //rt register
      ins.setParams(params);
    } catch (IrregularWriteOperationException e) {
      e.printStackTrace();
    }

    try {
      ins.pack();
      ins.ID();
      ins.EX();
      ins.WB();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
