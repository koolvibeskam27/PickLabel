package com.kamal.picklabel

fun buildZPL(
    name: String,
    wo: String,
    item: String,
    qty: String,
    lot: String,
    loc: String
): String {

    val sb = StringBuilder()

    sb.append("^XA")
    sb.append("^PW609")
    sb.append("^LL1624")
    sb.append("^LH0,0")
    sb.append("^CI28")

    // Fixed positions for main fields
    sb.append("^FO20,40^A0N,60,60^FDName: $name^FS")
    sb.append("^FO20,180^A0N,60,60^FDWO: $wo^FS")
    sb.append("^FO20,320^A0N,60,60^FDItem: $item^FS")
    sb.append("^FO20,460^A0N,60,60^FDQty: $qty^FS")

    // Dynamic Y position for optional fields
    var y = 600

    // LOT section
    if (lot.isNotBlank()) {
        sb.append("^FO20,$y^A0N,60,60^FDLot: $lot^FS")
        y += 140  // move down for barcode
        sb.append("^BY2,3,180")
        sb.append("^FO60,$y^BCN,180,Y,N,N^FD$lot^FS")
        y += 260  // move down for next section
    }

    // LOCATION section
    if (loc.isNotBlank()) {
        sb.append("^FO20,$y^A0N,60,60^FDLoc: $loc^FS")
        y += 140
        sb.append("^BY2,3,180")
        sb.append("^FO60,$y^BCN,180,Y,N,N^FD$loc^FS")
        y += 260
    }

    sb.append("^XZ")

    return sb.toString()
}
