package de.othr.dco

import akka.actor.typed.ActorSystem
import de.othr.dco.actor.TollSupervisor

object TollcollectApp extends App {

  val system = ActorSystem(TollSupervisor(), "toll-supervisor")

  system ! "hello-A3-1"
  system ! "hello-A93-1"
  // uncomment "fail" message if you want to test "Actor Supervision"
//  system ! "fail-A3"
  system ! "hello-A3-1"
  system ! "hello-A93-1"
  // uncomment "sensor" messages if you want to test "Actor Discovery" (and Stashing)
  system ! "sensor-A93-1-R_AB_123-enter"; Thread.sleep(100) // Ugh! Just simulation time needed for driving, otherwise truck gets messages unordered
  system ! "sensor-A93-2-R_AB_123-usage"; Thread.sleep(100) // Ugh! Just simulation time needed for driving, otherwise truck gets messages unordered
  system ! "sensor-A3-1-R_AB_123-usage"; Thread.sleep(100)  // Ugh! Just simulation time needed for driving, otherwise truck gets messages unordered
  system ! "sensor-A3-2-R_AB_123-exit"; Thread.sleep(100)   // Ugh! Just simulation time needed for driving, otherwise truck gets messages unordered
//  system ! "sensor-A3-2-EBE_X_987"
//  system ! "sensor-A3-2-NM_JK_505"
//  system ! "sensor-A3-2-SAD_XY_29"
//  system ! "sensor-A3-2-R_D_9932"
//  system ! "sensor-A3-2-NF_SY_432"
//  system ! "sensor-A3-2-CLP_EE_5"

}
