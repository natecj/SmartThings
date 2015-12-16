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

definition(
  name: "Multi Zone Mode Changer",
  namespace: "natecj",
  author: "Nathan Jacobson",
  description: "Change modes based on one or more switches being 'on' in multiple zones.",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
  section("Switches"){
    input "upstairsSwitches", "capability.switch", title: "Upstairs", multiple: true, required: false
    input "downstairsSwitches", "capability.switch", title: "Downstairs", multiple: true, required: false  
  }
  section("Person 1"){
    input "person1presence", "capability.presenceSensor", title: "Presence Sensor", multiple: false, required: false
    input "person1sleep", "capability.presenceSensor", title: "Sleep Sensor", multiple: false, required: false
  }
  section("Person 2"){
    input "person2presence", "capability.presenceSensor", title: "Presence Sensor", multiple: false, required: false
    input "person2sleep", "capability.presenceSensor", title: "Sleep Sensor", multiple: false, required: false
  }
  section("Person 3"){
    input "person3presence", "capability.presenceSensor", title: "Presence Sensor", multiple: false, required: false
    input "person3sleep", "capability.presenceSensor", title: "Sleep Sensor", multiple: false, required: false
  }
  section("Modes") {
    input "modeAllOn", "mode", title: "All Zones Active", defaultValue: "Home", required: false
    input "modeAllOff", "mode", title: "All Zones Inactive", defaultValue: "Away", required: false
    input "modeOnlyUpstairs", "mode", title: "Only Upstairs", defaultValue: "Night", required: false
    input "modeOnlyDownstairs", "mode", title: "Only Downstairs", defaultValue: "Day", required: false
  }
}

def installed() {
  initialize()
}

def updated() {
  unsubscribe()
  initialize()
}

def initialize() {
  subscribe(upstairsSwitches, "switch", changeHandler)
  subscribe(downstairsSwitches, "switch", changeHandler)
  subscribe(person1presence, "presence", changeHandler)
  subscribe(person1sleep, "presence", changeHandler)
  subscribe(person2presence, "presence", changeHandler)
  subscribe(person2sleep, "presence", changeHandler)
  subscribe(person3presence, "presence", changeHandler)
  subscribe(person3sleep, "presence", changeHandler)
}

def person1SleepHandler(evt) {
  if (person1presence.currentValue('presence') != 'present') {
    person1presence.arrived()
  }
  if (evt.value == "present") {

  } else {
  
  }
  updateMode()
}

def changeHandler(evt) {
  updateMode()
}

def updateMode() {
  def upstairsSwitchesActive = upstairsSwitches.any{ it.currentValue('switch') == 'on' }
  def downstairsSwitchesActive = downstairsSwitches.any{ it.currentValue('switch') == 'on' }
  
  def upstairsPerson1Active = person1sleep.latestValue("presence") == "present"
  def downstairsPerson1Active = person1presence.latestValue("presence") == "present"
    
  def upstairsPerson2Active = person2sleep.latestValue("presence") == "present"
  def downstairsPerson2Active = person2presence.latestValue("presence") == "present"
  
  def upstairsPerson3Active = person3sleep.latestValue("presence") == "present"
  def downstairsPerson3Active = person3presence.latestValue("presence") == "present"
  
  def upstairsActive = upstairsSwitchesActive || upstairsPerson1Active || upstairsPerson2Active || upstairsPerson3Active
  def downstairsActive = downstairsSwitchesActive || downstairsPerson1Active || downstairsPerson2Active || downstairsPerson3Active

  if (upstairsActive && downstairsActive) {
    myDebug "updateMode - All On"
    setLocationMode(modeAllOn)
  } else if (!upstairsActive && !downstairsActive) {
    myDebug "updateMode - All Off"
    setLocationMode(modeAllOff)
  } else if (upstairsActive && !downstairsActive) {
    myDebug "updateMode - Upstairs Only"
    setLocationMode(modeOnlyUpstairs)
  } else if (!upstairsActive && downstairsActive) {
    myDebug "updateMode - Downstairs Only"
    setLocationMode(modeOnlyDownstairs)
  }
}

def myDebug(message) {
  //log.debug message
}