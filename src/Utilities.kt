fun Array<Boolean>.toInteger() : Int {
    this.reverse() // For endianness
    var result = 0
    for (index in this.indices) {
        if (this[index]) {
            result = result or (1 shl index)
        }
    }
    return result
}

fun Array<Boolean>.toString(length: Int) : String {
    val result = Array(length) {'0'}
    for (index in this.indices) {
        if (this[index]) {
            result[index] = '1'
        }
    }
    return result.joinToString("")
}

fun Array<Boolean>.toBitList() : BitList {
    val result = BitList("")
    for (index in this.indices) {
        if (this[index]) {
            result.set(index)
        }
    }
    return result
}

fun Int.toBitList() : BitList {
    return BitList(Integer.toBinaryString(this))
}

fun boolArrayToIntArray(input : Array<Boolean>) : IntArray {
    val output = Array(input.size) {0}
    for (each in input.indices) {
        if (input[each]) {
            output[each] = 1
        } else output[each] = 0
    }
    return output.toIntArray()
}

fun intArrayToBitList(input : IntArray) : BitList {
    val output = BitList("")
    for (each in input.indices) output[each] = input[each] != 0
    return output
}