/**
 *  Copyright 2015 Nathan Jacobson <natecj@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

/**
 *  Original Author: LGKahn kahn-st@lgk.com
 *  Source: http://mail.lgk.com/lgkvirtualgaragedoorsmartappv2.txt
 *  version 2 user defineable timeout before checking if door opened or closed correctly. Raised default to 25 secs. You can reduce it to 15 secs. if you have custom simulated door with < 6 sec wait.
 *
 */

definition(
  name: "Virtual Garage Door",
  namespace: "natecj",
  author: "Nathan Jacobson",
  description: "Sync a Virtual Garage Door device with a Tilt/Contact Sensor and a Switch/Relay for single-device control of a garage door",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
	section("Choose the device that controls if the door is open/closed"){
		input "opener", "capability.switch", title: "Door Button", required: true
	}
	section("Choose the device that senses if the door is open/closed"){
		input "sensor", "capability.contactSensor", title: "Door Sensor", required: true
	}

	section("Choose the Virtual Garage Door device"){
		input "virtualgd", "capability.doorControl", title: "Virtual Door Opener", required: true
	}
	section("Choose the Virtual Garage Door device (same as above device)?"){
		input "virtualgdbutton", "capability.contactSensor", title: "Virtual Door Sensor", required: true
	}

  section("Timeout before checking if the door opened or closed correctly?"){
		input "checkTimeout", "number", title: "Door Operation Check Timeout?", required: true, defaultValue: 25
	}

  section( "Notifications" ) {
    input("recipients", "contact", title: "Send notifications as...") {
      input "sendPushMessage", "enum", title: "A Push Notification?", options: ["Yes", "No"], required: false
      input "phone1", "phone", title: "A Text Message?", required: false
    }
  }
}

def installed() {
  def realgdstate = sensor.currentContact
  def virtualgdstate = virtualgd.currentContact
  //log.debug "in installed ... current state=  $realgdstate"
  //log.debug "gd state= $virtualgd.currentContact"

	subscribe(sensor, "contact", contactHandler)
  subscribe(virtualgdbutton, "contact", virtualgdcontactHandler)

  // sync them up if need be set virtual same as actual
  if (realgdstate != virtualgdstate) {
    if (realgdstate == "open")
      virtualgd.open()
    else
      virtualgd.close()
  }
}

def updated() {
  def realgdstate = sensor.currentContact
  def virtualgdstate = virtualgd.currentContact
  //log.debug "in updated ... current state=  $realgdstate"
  //log.debug "in updated ... gd state= $virtualgd.currentContact"

	unsubscribe()
	subscribe(sensor, "contact", contactHandler)
  subscribe(virtualgdbutton, "contact", virtualgdcontactHandler)

  // sync them up if need be set virtual same as actual
  if (realgdstate != virtualgdstate) {
    if (realgdstate == "open") {
      log.debug "opening virtual door"
      mysend("Virtual Garage Door Opened!")
      virtualgd.open()
    } else {
      virtualgd.close()
      log.debug "closing virtual door"
      mysend("Virtual Garage Door Closed!")
    }
  }
  // for debugging and testing uncomment  temperatureHandlerTest()
}

def contactHandler(evt) {
  def virtualgdstate = virtualgd.currentContact
  // how to determine which contact
  //log.debug "in contact handler for actual door open/close event. event = $evt"

  if("open" == evt.value) {
    // contact was opened, turn on a light maybe?
    log.debug "Contact is in ${evt.value} state"
    // reset virtual door if necessary
    if (virtualgdstate != "open") {
      mysend("Garage Door Opened Manually syncing with Virtual Garage Door!")
      virtualgd.open()
    }
  }
  if("closed" == evt.value) {
    // contact was closed, turn off the light?
    log.debug "Contact is in ${evt.value} state"
    //reset virtual door
    if (virtualgdstate != "closed") {
      mysend("Garage Door Closed Manually syncing with Virtual Garage Door!")
      virtualgd.close()
    }
  }
}

def virtualgdcontactHandler(evt) {
  // how to determine which contact
  def realgdstate = sensor.currentContact
  //log.debug "in virtual gd contact/button handler event = $evt"
  //log.debug "in virtualgd contact handler check timeout = $checkTimeout"

  if("open" == evt.value) {
    // contact was opened, turn on a light maybe?
    log.debug "Contact is in ${evt.value} state"
    // check to see if door is not in open state if so open
    if (realgdstate != "open") {
      log.debug "opening real gd to correspond with button press"
      mysend("Virtual Garage Door Opened syncing with Actual Garage Door!")
      opener.on()
      runIn(checkTimeout, checkIfActuallyOpened)
    }
  }
  if("closed" == evt.value) {
    // contact was closed, turn off the light?
    log.debug "Contact is in ${evt.value} state"
    if (realgdstate != "closed") {
      log.debug "closing real gd to correspond with button press"
      mysend("Virtual Garage Door Closed syncing with Actual Garage Door!")
      opener.on()
      runIn(checkTimeout, checkIfActuallyClosed)
    }
  }
}

private mysend(msg) {
  if (location.contactBookEnabled) {
    log.debug("sending notifications to: ${recipients?.size()}")
    sendNotificationToContacts(msg, recipients)
  } else {
    if (sendPushMessage != "No") {
      log.debug("sending push message")
      sendPush(msg)
    }
    if (phone1) {
      log.debug("sending text message")
      sendSms(phone1, msg)
    }
  }
  log.debug msg
}

def checkIfActuallyClosed() {
  def realgdstate = sensor.currentContact
  def virtualgdstate = virtualgd.currentContact
  //log.debug "in checkifopen ... current state=  $realgdstate"
  //log.debug "in checkifopen ... gd state= $virtualgd.currentContact"

  // sync them up if need be set virtual same as actual
  if (realgdstate == "open" && virtualgdstate == "closed") {
    log.debug "opening virtual door as it didnt close.. beam probably crossed"
    mysend("Resetting Virtual Garage Door to Open as real door didn't close (beam probably crossed)!")
    virtualgd.open()
  }
}

def checkIfActuallyOpened() {
  def realgdstate = sensor.currentContact
  def virtualgdstate = virtualgd.currentContact
  //log.debug "in checkifopen ... current state=  $realgdstate"
  //log.debug "in checkifopen ... gd state= $virtualgd.currentContact"

  // sync them up if need be set virtual same as actual
  if (realgdstate == "closed" && virtualgdstate == "open") {
    log.debug "opening virtual door as it didnt open.. track blocked?"
    mysend("Resetting Virtual Garage Door to Closed as real door didn't open! (track blocked?)")
    virtualgd.close()
  }
}

