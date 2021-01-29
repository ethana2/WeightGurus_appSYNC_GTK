import java.util.*

class BitList() : BitSet() {

    constructor(s: String) : this() {
        val string = s.reversed()
        for (index in string.indices) {
            if (string[index] == '1') {
                this.set(index, true)
            }
        }
    }

    override fun toString(): String {
        val charArray = CharArray(this.length()) { '0' }
        for (index in (charArray.indices)) {
            if (this[index]) {
                charArray[index] = '1'
            } else {
                charArray[index] = '0'
            }
        }
        var result = charArray.joinToString("").reversed()
        if (result == "") {
            result = "0"
        }
        return result
    }

    fun toInt(): Int {
        return Integer.parseInt(this.toString(), 2)
    }

    fun toBoolArray(length: Int): Array<Boolean> {
        val result = Array(length) { false }
        for (index in result.indices) {
            if (this[index]) {
                result[index] = true
            }
        }
        return result
    }

    // This is for breaking identity
    fun copy(): BitList {
        return (BitList(this.toString()))
    }

    operator fun inc(): BitList {
        return (this.toInt() + 1).toBitList()
    }

    operator fun dec(): BitList {
        return (this.toInt() - 1).toBitList()
    }

    operator fun minus(b: Int): BitList {
        return (this.toInt() - b).toBitList()
    }

    operator fun minus(b: BitList): BitList {
        return (this.toInt() - b.toInt()).toBitList()
    }

    operator fun plus(b: Int): BitList {
        return (this.toInt() + b).toBitList()
    }

    operator fun plus(b: BitList): BitList {
        return (this.toInt() + b.toInt()).toBitList()
    }

    infix fun bitwiseAnd(b: BitList): BitList {
        val a = BitList(this.toString()) // This is here to break identity because of how BitSet works.
        a.and(b)
        return a
    }
}