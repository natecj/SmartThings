/**
 *  Copyright 2015 Nathan Jacobson
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
definition(
  name: "Auto Door Lock",
  namespace: "natecj",
  author: "Nathan Jacobson",
  description: "Automatically locks a specific door after X minutes. Use the optional contact sensor to send a notification if left open and unable to auto-lock.",
  category: "Safety & Security",
  iconUrl: "http://www.gharexpert.com/mid/4142010105208.jpg",
  iconX2Url: "http://www.gharexpert.com/mid/4142010105208.jpg"
)

preferences {
  section("Select the door lock:") {
    input "lock", "capability.lock", required: true
  }
  section("Select the door contact sensor:") {
    input "contact", "capability.contactSensor", required: false
  }
  section("Automatically lock the door...") {
    input "minutesLater", "number", title: "Delay (in minutes):", required: true
  }
  section( "Notifications" ) {
    input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes", "No"]], required: false
    input "phoneNumber", "phone", title: "Enter phone number to send text notification.", required: false
  }
}

def installed() {
  initialize()
}

def updated() {
  unsubscribe()
  unschedule()
  initialize()
}

def initialize() {
  log.debug "Settings: ${settings}"
  subscribe(lock, "lock", doorHandler, [filterEvents: false])
  subscribe(lock, "unlock", doorHandler, [filterEvents: false])
}

def sendMessage(message) {
  if ( sendPushMessage == "Yes" ) {
    log.debug("Sending Push Notification...")
    sendPush(message)
  }
  if ( phoneNumber != "0" ) {
    log.debug("Sending text message...")
    sendSms(phoneNumber, message)
  }
}

def checkLockDoor() {
  if (lock.latestValue("lock") == "locked")
    sendMessage("${lock} was auto-locked after ${minutesLater} minutes")
  else
    sendMessage("${lock} failed to auto-lock after ${minutesLater} minutes")
}

def lockDoor() {
  String lockState = lock.latestValue("lock")
  String contactState = contact ? contact.latestValue("contact") : "closed"
  log.debug "Attempting to lock the door... (Current State: ${lockState}, ${contactState})"

  if (contactState == "open") {
    sendMessage("${lock} has been open and ${lockState} for ${minutesLater} minutes")
  } else if (lockState == "unlocked" && contactState == "closed") {
    lock.lock()
    runIn(60, checkLockDoor)
  }
}

def doorHandler(evt) {
  if (evt.value == "locked" && (!contact || contact.latestValue("contact") == "closed")) {
    unschedule(lockDoor) // nothing to do when door is locked (and closed if there is a contact sensor)
  } else if (evt.value == "unlocked") {
    runIn((minutesLater * 60), lockDoor) // something to do when door is unlocked
  } else {
    log.debug "Unknown Event: ${evt.value}"
  }
}

