package wallace

import partialprod._

import io._
import chisel3.iotesters.PeekPokeTester
import chisel3.util._
import chisel3.{Bundle, Input, Module, Output, UInt, _}

import java.io.PrintWriter
import java.io.File

import scala.io.Source

class Wallace(m: Int, n: Int, myarch: List[Int], inedges: Map[List[Int], List[Int]], outedges: Map[List[Int], List[Int]], res: Map[Int, List[Int]]) extends Module {
  val io = IO(new Bundle {
    val pp = Input(Vec(n, UInt(m.W)))
    val augend = Output(UInt((n + m).W))
    val addend = Output(UInt((n + m).W))
  })

  var ValueMap = Map[List[Int], Data]()

  for (i <- 0 until n) {
    for (j <- 0 until m) {
      var tmpx = i
      var tmpy = i + j
      var fy = tmpy
      var fx = 0
      if (tmpy >= m) {
        var move = tmpy - m + 1
        fx = tmpx - move
      } else {
        fx = tmpx
      }
      ValueMap += List(fx, fy) -> io.pp(i)(j)
    }
  }

  val len = myarch.length
  var depth = 0
  var ind = 500
  var i = 0
  var cnt = new Array[Int](256)
  while (i < len) {
    if (myarch(i) > ind) {
      depth += 1
      for (j <- 0 until (n + m)) {
        cnt(j) = 0
      }
    }
    ind = myarch(i)
    cnt(myarch(i)) += 1

    if (myarch(i + 1) == 0) {
      val cmp22 = Module(new HalfAdder)
      val tmpin = inedges(List(myarch(i), depth, cnt(myarch(i))))
      cmp22.io.a := ValueMap(List(tmpin(0), tmpin(1)))
      cmp22.io.b := ValueMap(List(tmpin(0) + 1, tmpin(1)))
      val tmpout = outedges(List(myarch(i), depth, cnt(myarch(i))))
      ValueMap += List(tmpout(0), tmpout(1)) -> cmp22.io.s
      ValueMap += List(tmpout(2), tmpout(3)) -> cmp22.io.co
    }
    else if (myarch(i + 1) == 1) {
      val cmp32 = Module(new FullAdder)
      val tmpin = inedges(List(myarch(i), depth, cnt(myarch(i))))
      cmp32.io.a := ValueMap(List(tmpin(0), tmpin(1)))
      cmp32.io.b := ValueMap(List(tmpin(0) + 1, tmpin(1)))
      cmp32.io.ci := ValueMap(List(tmpin(0) + 2, tmpin(1)))
      val tmpout = outedges(List(myarch(i), depth, cnt(myarch(i))))
      ValueMap += List(tmpout(0), tmpout(1)) -> cmp32.io.s
      ValueMap += List(tmpout(2), tmpout(3)) -> cmp32.io.co
    }
    i += 2
  }
  val res0 = (0 until (m + n - 1)).map(i => Wire(UInt(1.W)))
  val res1 = (0 until (m + n - 1)).map(i => Wire(UInt(1.W)))


  for (j <- 0 until (m + n - 1)) {
    res0(j) := ValueMap(List(res(j)(0), j))
    if (res(j)(1) == -1) {
      res1(j) := 0.asUInt()
    } else {
      res1(j) := ValueMap(List(res(j)(1), j))
    }
  }
  io.augend := res0.reverse.reduce(Cat(_, _))
  io.addend := res1.reverse.reduce(Cat(_, _))
}

class Wallace1(m: Int, n: Int, myarch: List[Int], inedges: Map[List[Int], List[Int]], outedges: Map[List[Int], List[Int]], res: Map[Int, List[Int]]) extends Module {
  val io = IO(new Bundle {
    val pp = Input(Vec(n, UInt(m.W)))
    val accmulatend = Input(UInt((n + m - 1).W))
    val augend = Output(UInt((n + m).W))
    val addend = Output(UInt((n + m).W))
  })

  var ValueMap = Map[List[Int], Data]()

  for (i <- 0 until n) {
    for (j <- 0 until m) {
      var tmpx = i
      var tmpy = i + j
      var fy = tmpy
      var fx = 0
      if (tmpy >= m) {
        var move = tmpy - m + 1
        fx = tmpx - move
      } else {
        fx = tmpx
      }
      ValueMap += List(fx, fy) -> io.pp(i)(j)
    }
  }

  for (i <- 0 until (n + m - 1)) {
    var move = 0;
    if (m >= n) {
      if (i < n - 1) {
        move = n - 1 - i;
      }
      else if (i >= m) {
        move = i - m + 1;
      }
      else {
        move = 0;
      }
    }
    else {
      var min = n - m;
      if (i < m - 1) {
        move = m - 1 - i + min;
      }
      else if (i > n - 1) {
        move = i - n + 1 + min;
      }
      else {
        move = min;
      }
    }
    ValueMap += List(n - move, i) -> io.accmulatend(i)
  }

  val len = myarch.length
  var depth = 0
  var ind = 500
  var i = 0
  var cnt = new Array[Int](256)
  while (i < len) {
    if (myarch(i) > ind) {
      depth += 1
      for (j <- 0 until (n + m)) {
        cnt(j) = 0
      }
    }
    ind = myarch(i)
    cnt(myarch(i)) += 1

    if (myarch(i + 1) == 0) {
      val cmp22 = Module(new HalfAdder)
      val tmpin = inedges(List(myarch(i), depth, cnt(myarch(i))))
      cmp22.io.a := ValueMap(List(tmpin(0), tmpin(1)))
      cmp22.io.b := ValueMap(List(tmpin(0) + 1, tmpin(1)))
      val tmpout = outedges(List(myarch(i), depth, cnt(myarch(i))))
      ValueMap += List(tmpout(0), tmpout(1)) -> cmp22.io.s
      ValueMap += List(tmpout(2), tmpout(3)) -> cmp22.io.co
    }
    else if (myarch(i + 1) == 1) {
      val cmp32 = Module(new FullAdder)
      val tmpin = inedges(List(myarch(i), depth, cnt(myarch(i))))
      cmp32.io.a := ValueMap(List(tmpin(0), tmpin(1)))
      cmp32.io.b := ValueMap(List(tmpin(0) + 1, tmpin(1)))
      cmp32.io.ci := ValueMap(List(tmpin(0) + 2, tmpin(1)))
      val tmpout = outedges(List(myarch(i), depth, cnt(myarch(i))))
      ValueMap += List(tmpout(0), tmpout(1)) -> cmp32.io.s
      ValueMap += List(tmpout(2), tmpout(3)) -> cmp32.io.co
    }
    i += 2
  }
  val res0 = (0 until (m + n)).map(i => Wire(UInt(1.W)))
  val res1 = (0 until (m + n)).map(i => Wire(UInt(1.W)))


  for (j <- 0 until (m + n)) {
    res0(j) := ValueMap(List(res(j)(0), j))
    if (res(j)(1) == -1) {
      res1(j) := 0.asUInt()
    } else {
      res1(j) := ValueMap(List(res(j)(1), j))
    }
  }
  io.augend := res0.reverse.reduce(Cat(_, _))
  io.addend := res1.reverse.reduce(Cat(_, _))
}

class PartialProdWallaceTree(m: Int, n: Int, myarch: List[Int], inedges: Map[List[Int], List[Int]], outedges: Map[List[Int], List[Int]], res: Map[Int, List[Int]]) extends Module {
  val io = IO(new Bundle {
    val multiplicand = Input(UInt(m.W))
    val multiplier = Input(UInt(n.W))
    val augend = Output(UInt((n + m).W))
    val addend = Output(UInt((n + m).W))
  })

  val pp = Module(new PartialProd(m, n))
  pp.io.multiplicand := io.multiplicand
  pp.io.multiplier := io.multiplier

  val wt = Module(new Wallace(m, n, myarch, inedges, outedges, res))
  wt.io.pp := pp.io.outs

  io.augend := wt.io.augend
  io.addend := wt.io.addend
}



object test{
  val usage = """
      Usage: generate [--wallace-file filename1] [--target-dir targetdir]
  """
  def main(args: Array[String]): Unit = {
    
    if (args.length == 0) println(usage)
    
    val arglist = args.toList
    val optionNames = arglist.filter(s => s.contains('-'))

    val argmap = (0 until arglist.size / 2).map(i => arglist(i * 2) -> arglist(i * 2 + 1)).toMap

    val filename1 = argmap("--wallace-file")
    val targetdir = argmap("--target-dir")

    val filecontent = ReadWT.readFromWTTxt(filename1)

    val m = ReadWT.getBits(filecontent)(0)
    val n = ReadWT.getBits(filecontent)(1)

    val numcells = ReadWT.getNumCells(filecontent)(0)

    val myarch = ReadWT.getArch(filecontent)

    val depth = ReadWT.getDepth(myarch)

    val inedges = ReadWT.getIn(m, n, myarch)

    val outedges = ReadWT.getOut(m, n, myarch)

    val res = ReadWT.getRes(m, n, myarch)

    val topDesign = () => new PartialProdWallaceTree(m, n, myarch, inedges, outedges, res)
    chisel3.Driver.execute(Array("-td", targetdir), topDesign)
    // iotesters.Driver.execute(Array("-tgvo", "on", "-tbn", "verilator"), topDesign) {
    //   c => new WallaceTester(c)
    // }

    // iotesters.Driver.execute(Array("-tgvo", "on", "-tbn", "verilator"), () => new Wallace(m, n, myarch, inedges, outedges, res)) {
    //   c => new WallaceTester(c)
    // }
  }
}

class WallaceTester(c: Wallace) extends PeekPokeTester(c) {
  poke(c.io.pp(0), 0)
  poke(c.io.pp(1), 5)
  poke(c.io.pp(2), 0)
  poke(c.io.pp(3), 0)
  
  step(1)
  //println("The addend of parallel prefix adder is: " + peek(c.io.addend).toString())
  //println("The result of parallel prefix adder is: " + peek(c.io.outs).toString())

  println("The result of 5*2 with is: " + peek(c.io.augend).toString())

  expect(c.io.augend, 8)
}