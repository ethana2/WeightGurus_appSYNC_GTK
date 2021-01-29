class FieldSet {
    val weight = Weight()
    val fat = Fat()
    val muscle = Muscle()
    val water = Water()
    val mode = Mode()
    var type = FieldSetType.ALL_ZEROS

    val hamming74 = Hamming74()
    var codeWordPlane = Array (70) {false}
    var codeWordsList = Array(10) {List (7) {false} }
    var codeWordsArray = Array(10) {Array (7) {false} }
    var dataNibbles = Array(10) {Array (4) {false} }
    var dataPlane = Array (40) {false}

    fun setStateFromCodeWordPlane(codeWordPlane: Array<Boolean>) {
        codeWordsList = codeWordPlane.toList().chunked(7).toTypedArray()
        for (index in 0..9) {
            val codeWord = BitList(codeWordsList[index].toTypedArray().reversedArray().toString(7)) // This line is broken.
            hamming74.setStateByCodeword(codeWord)
            dataNibbles[index] = hamming74.data.toBoolArray(4).reversedArray()
        }
        dataPlane = dataNibbles.flatten().toTypedArray()

        weight.setStateFromBits(dataPlane)
        fat.setStateFromBits(dataPlane)
        muscle.setStateFromBits(dataPlane)
        water.setStateFromBits(dataPlane)
        mode.setStateFromBits(dataPlane)
        updateType()
    }

    fun setStateFromFields(weightVal: Float, fatVal: Float, muscleVal: Float, waterVal: Float, modeVal: Modes) {
        weight.setStateFromValue(dataPlane, weightVal)
        fat.setStateFromValue(dataPlane, fatVal)
        muscle.setStateFromValue(dataPlane, muscleVal)
        water.setStateFromValue(dataPlane, waterVal)
        mode.setStateFromValue(dataPlane, modeVal)
        updateType()

        for (index in 0..9) {
            dataNibbles[index] = dataPlane.copyOfRange(4*index, 4*index +4).reversedArray()
            hamming74.setStateByData(dataNibbles[index].toBitList())
            codeWordsArray[index] = hamming74.codeword.toBoolArray(7)
        }
        codeWordPlane = codeWordsArray.flatten().toTypedArray()

    }

    fun updateType() {
        var fieldsPresent = 0
        if (weight.status == FieldStatus.PRESENT) fieldsPresent++
        if (fat.status == FieldStatus.PRESENT) fieldsPresent++
        if (muscle.status == FieldStatus.PRESENT) fieldsPresent++
        if (water.status == FieldStatus.PRESENT) fieldsPresent++

        type = when(fieldsPresent) {
            0    -> FieldSetType.ALL_ZEROS   // Synthetic or uninitialized
            1    -> FieldSetType.WEIGHT_ONLY // Weight, mode are all that's displayed on these scales for unknown users
            4    -> FieldSetType.FULL        // This is what is expected when a known user has their body comp. read
            else -> FieldSetType.SYNTHETIC   // Not from an actual scale, but generated by code like this.
            // We can use ALL_ZEROES/SYNTHETIC status as a flags for displaying or logging detailed debug information.
        }
    }

    // Needs to account for present and absent.
    override fun toString() : String {
        updateType()
        val fieldSetTypeString = "Field set type is $type\n"
        val stringArray = Array (5) {""}
        stringArray[0] = "Weight is ${if (weight.status == FieldStatus.PRESENT) {weight.value} else {"ABSENT"}}\n"
        stringArray[1] = "Fat is ${if (fat.status == FieldStatus.PRESENT) {fat.value} else {"ABSENT"}}\n"
        stringArray[2] = "Muscle is ${if (muscle.status == FieldStatus.PRESENT) {muscle.value} else {"ABSENT"}}\n"
        stringArray[3] = "Water is ${if (water.status == FieldStatus.PRESENT) {water.value} else {"ABSENT"}}\n"
        stringArray[4] = "Mode is ${mode.value}\n"
        // Need updated

        return fieldSetTypeString + stringArray.joinToString("")
    }

    // I didn't use an interface for Field because extract and inject involve different data types between Fields.
    open class Field {
        open val index = 0
        open val length = 1
        open var status = FieldStatus.ABSENT
    }

    open class FloatField : Field() {
        open val factor = 10f
        open val offset = 0f
        open var value = 0f

        open fun extractBits(dataPlane: Array<Boolean>) : Array<Boolean> {
            return dataPlane.copyOfRange(index, index + length)
        }

        open fun insertBits(dataPlane: Array<Boolean>, fieldBits : Array<Boolean>) : Int {
            fieldBits.copyInto(dataPlane, index)
            return 0
        }

        fun setStateFromBits(dataPlane : Array<Boolean>) {
            // I'm NOT chaining math function calls here because doing so provides WRONG DATA for some reason
            // Also these floats should probably be replaced with bigDecimals at some point
            val bits = extractBits(dataPlane)
            val cardinality = bits.toBitList().cardinality()
            status = if (cardinality == 0) {
                FieldStatus.ABSENT
            } else {
                FieldStatus.PRESENT
            }
            val int = bits.toInteger()
            val float = int.toFloat()
            val floatOverFactor = float / factor
            this.value = floatOverFactor + offset
        }

        fun setStateFromValue(dataPlane : Array<Boolean>, value : Float) : Array<Boolean>{
            this.value = value
            this.status = FieldStatus.PRESENT
            val offsetValue = value - offset
            val multipliedValue = offsetValue * factor
            val injectionInt = multipliedValue.toInt()
            val bitList = BitList(Integer.toBinaryString(injectionInt))
            val bits = bitList.toBoolArray(length).reversedArray()
            insertBits(dataPlane, bits)
            return dataPlane
        }
    }

    class Weight : FloatField() {
        override val index = 0 // Setting this index again here for readability
        override val length = 11
    }

    class Fat : FloatField() {
        override val index = 11
        override val length = 10
    }

    class Muscle : FloatField() {
        override val index = 21
        override val length = 9
        override val offset = 14.9f
    }

    class Water : FloatField() {
        override val index = 30
        override val length = 7
        override val factor = 2.0f
        override val offset = 18.0f

        override fun extractBits(dataPlane: Array<Boolean>) : Array<Boolean> {
            val dataPlaneBits =  dataPlane.copyOfRange(index, index + 9) // Note, grabbing 9 bits instead of 7
            val fieldBits = Array (7) {false}
            dataPlaneBits.copyOfRange(0,3).copyInto(fieldBits, 0)
            fieldBits[4] = dataPlaneBits[4] or dataPlaneBits[6]
            fieldBits[5] = dataPlaneBits[5] or dataPlaneBits[7]
            fieldBits[6] = dataPlaneBits[8]
            return fieldBits
        }

        override fun insertBits(dataPlane: Array<Boolean>, fieldBits : Array<Boolean>) : Int {
            val dataPlaneBits = Array (9) {false}
            fieldBits.copyOfRange(0,6).copyInto(dataPlaneBits, 0)
            dataPlaneBits[8] = fieldBits[6]
            dataPlaneBits.copyInto(dataPlane, index)
            return 0
        }
    }

    class Mode : Field() {
        override val index = 39
        override var status = FieldStatus.ABSENT
        var value = Modes.LB

        fun setStateFromBits(dataPlane : Array<Boolean>) {
            status = FieldStatus.AMBIGUOUS
            /*
            The reason I'm setting this ambiguous regardless of content is because v1 barcodes don't contain its data.
            It's literally only able to be rebuilt from parity information, same as the LSB of the water field.
            Because of this, it's the least trustworthy information in the code, but at least it was already useless.
            I capitalize on this for synthetic payload detection below.
             */
            value = if (dataPlane[index]) {
                Modes.KG
            } else {
                Modes.LB
            }
        }

        fun setStateFromValue(dataPlane : Array<Boolean>, value : Modes): Array<Boolean> {
            this.value = value
            val bits = Array (1) {false}
            bits[0] = value == Modes.KG
            bits.copyInto(dataPlane, index)
            status = FieldStatus.AMBIGUOUS // See previous comment
            return dataPlane
        }
    }

    enum class Modes {
        KG, LB
    }

    enum class FieldStatus {
        PRESENT, ABSENT, AMBIGUOUS
    }

    enum class FieldSetType {
        ALL_ZEROS, WEIGHT_ONLY, SYNTHETIC, FULL
    }
}