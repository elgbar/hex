package no.elg.hex

import no.elg.hex.util.delegate.PreferenceDelegate

object Settings {

  var confirmEndTurn by PreferenceDelegate(true)
  var confirmSurrender by PreferenceDelegate(true)
//  var testString by PreferenceDelegate("ye")
//  var testChar by PreferenceDelegate('t')
//  var testInt by PreferenceDelegate(1)
//  var testLong by PreferenceDelegate(1L)
//  var testFloat by PreferenceDelegate(1f)
//  var testDouble by PreferenceDelegate(1.0)
//  var testShort by PreferenceDelegate<Short>(1)
//  var testByte by PreferenceDelegate<Byte>(1)
}
