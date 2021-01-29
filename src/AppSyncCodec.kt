internal class AppSyncCodec () {
    val hamming74 = Hamming74()
    var codeWordsList = Array(10) {List<Boolean>(7) {false} }
    var codeWordsArray = Array(10) {Array<Boolean> (7) {false} }
    var dataNibbles = Array(10) {Array<Boolean> (4) {false} }
    var dataPlane = Array<Boolean> (40) {false}

    fun setStateFromCodeWordPlane(codeWordPlane: Array<Boolean>): Array<Boolean> {
        codeWordsList = codeWordPlane.toList().chunked(7).toTypedArray()
        for (index in 0..9) {
            val codeWord = BitList(codeWordsList[index].toString())
            hamming74.setStateByCodeword(codeWord)
            dataNibbles[index] = hamming74.data.toBoolArray(4).reversedArray()
        }
        dataPlane = dataNibbles.flatten().toTypedArray()

        return dataPlane
    }

    fun setStateFromFields(dataPlane: Array<Boolean>): Array<Boolean> {
        //dataPlane = Array<Boolean> (40) {false}


        for (index in 0..9) {
            dataNibbles[index] = dataPlane.copyOfRange(4*index, 4*index +4).reversedArray()
            hamming74.setStateByData(dataNibbles[index].toBitList())
            codeWordsArray[index] = hamming74.codeword.toBoolArray(7)
        }
        val codeWordPlane = codeWordsArray.flatten().toTypedArray()

        return codeWordPlane
    }
}