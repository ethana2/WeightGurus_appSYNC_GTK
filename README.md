# WeightGurus_appSYNC_GTK

The purpose of this project is to sync body composition data from WeightGurus AppSYNC body composition scale barcodes. I hope to eventually see it included in OpenScale: https://github.com/oliexdev/openScale

This is free/libre open source software which can be used and distributed in accordance with the GNU General Public License version 2, or at your option, any later version.

At present, it can be tested on Linux with GNOME by setting up GTK Java bindings and replacing the image file referenced on line 10 of https://github.com/ethana2/WeightGurus_appSYNC_GTK/blob/master/src/GtkMain.kt with any image containing a camera shot of this model of scale displaying an AppSYNC barcode. When presented with such an image, it will spit out weight, body fat, water mass, muscle mass, and metric/imperial mode on the terminal. Right now it only supports one model of scale but I've purchased a second, different model to make its support more broad. It's written in Kotlin and uses OpenCV to facilitate porting to Android, which will be required to make it useful to end-users. It can be used in its current form not only to extract data from barcodes but also to generate and display barcodes containing arbitrary user data for testing purposes.

![Screenshot from 2021-03-08 16-50-30](https://user-images.githubusercontent.com/7855497/110408945-b1422b80-8043-11eb-9318-14889d392669.png)
