import org.gnome.gtk.*
import org.gnome.gtk.Image


fun main(args: Array<String>) {
    Gtk.init(args)

    val fieldSetB = FieldSet()
    val barcodeB = Barcode()
    barcodeB.setStateFromImage("/home/ethan/Desktop/CircleDetectTest.jpg")
    fieldSetB.setStateFromCodeWordPlane(barcodeB.codeWordPlane)
    println(fieldSetB)
    val imageB = barcodeB.getImageFile()

    val i = Image(imageB)
    val w = Window()
    w.setTitle("Rendered image")
    val v = VBox(false, 3)
    v.add(i)
    w.add(v)
    w.showAll()
    w.connect(Window.DeleteEvent { source, event ->
        Gtk.mainQuit()
        false
    })
    Gtk.main()
}