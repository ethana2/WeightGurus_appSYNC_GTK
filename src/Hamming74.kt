class Hamming74 {
    val codeGeneratorMatrix = arrayOf(
            BitList("1011"),
            BitList("1101"),
            BitList("0001"),
            BitList("1110"),
            BitList("0010"),
            BitList("0100"),
            BitList("1000")) // To read as a matrix, read right to left.
    val dataBitMasks = arrayOf(
            BitList("0000100"),
            BitList("0010000"),
            BitList("0100000"),
            BitList("1000000"))
    val parityCheckMatrix = arrayOf(
            BitList("1010101"),
            BitList("1100110"),
            BitList("1111000"))
    var status = Statuses.ERROR
    var data = BitList("0000000")
    var codeword = BitList("0000000")

    fun setStateByData(data: BitList) {
        this.data = data
        codeword = multiplyMatrices(codeGeneratorMatrix, data)
    }

    fun setStateByCodeword(codeword: BitList) {
        // Eventually, once we get a perfect payload, latch it in and ignore new incoming data
        this.codeword = codeword
        val fixedCodeword = fix(codeword)
        when (hammingDistance(codeword, fixedCodeword)) {
            0 -> {
                data = multiplyMatrices(dataBitMasks, codeword)
                status = Statuses.SUCCESS
            }
            1 -> {
                data = multiplyMatrices(dataBitMasks, fixedCodeword)
                status = Statuses.WARNING
            }
            else -> {
                data = BitList("0")
                status = Statuses.ERROR
            }
        }
    }

    // Returns true if the number of digits that are 1 is odd.
    fun parity(n: BitList): Boolean {
        var i = BitList(n.toString())
        var parity = false
        while (i != BitList("0000000")) {
            parity = !parity
            i = i bitwiseAnd (i - 1)
        }
        return parity
    }

    fun fix(n: BitList): BitList {
        val n = n.copy()
        val product = multiplyMatrices(parityCheckMatrix, n).toInt()
        if (product != 0) {
            n.flip(product - 1)
        }
        return n
    }

    // Multiplies a 2d matrix with a 1d matrix in that order.
    fun multiplyMatrices(operand: Array<BitList>, input: BitList): BitList {
        val result = BitList("0000000")
        for (index in operand.indices) {
            if (parity(operand[index] bitwiseAnd input)) {
                result.set(index, true)
            }
        }
        return result
    }

    fun hammingDistance(a: BitList, b: BitList) : Int {
        val a = a.copy()
        var bits = 0
        a.xor(b)
        for (each in 0..7) {
            if (a[each]) {
                bits++
            }
        }
        return bits
    }

    enum class Statuses {
        SUCCESS, WARNING, ERROR
    }
}