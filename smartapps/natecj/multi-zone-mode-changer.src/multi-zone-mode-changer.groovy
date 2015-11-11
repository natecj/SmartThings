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
  section("Person 1"){
    input "person1upstairs", "capability.switch", title: "Upstairs Switch", multiple: true
    input "person1downstairs", "capability.switch", title: "Downstairs Switch", multiple: true
    input "person1presence", "capability.presenceSensor", title: "Presence Sensor", multiple: true
  }
  section("Person 2"){
    input "person2upstairs", "capability.switch", title: "Upstairs Switch", multiple: true
    input "person2downstairs", "capability.switch", title: "Downstairs Switch", multiple: true
    input "person2presence", "capability.presenceSensor", title: "Presence Sensor", multiple: true
  }
	section("Modes") {
	  input "modeAllOn", "mode", title: "All On", defaultValue: "Home"
	  input "modeAllOff", "mode", title: "All Off", defaultValue: "Away"
	  input "modeOnlyUpstairs", "mode", title: "Only Upstairs", defaultValue: "Night"
	  input "modeOnlyDownstairs", "mode", title: "Only Downstairs", defaultValue: "Day"
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
	subscribe(person1upstairs, "switch", switchHandler)
	subscribe(person1downstairs, "switch", switchHandler)
	subscribe(person2upstairs, "switch", switchHandler)
	subscribe(person2downstairs, "switch", switchHandler)
}

def switchHandler(evt) {
  updateMode()
  updatePresence()
}

def updateMode() {
  def upstairsSwitches = person1upstairs + person2upstairs
  def upstairsActive = upstairsSwitches.any{ it.currentValue('switch') == 'on' }
  def downstairsSwitches = person1downstairs + person2downstairs
  def downstairsActive = downstairsSwitches.any{ it.currentValue('switch') == 'on' }

  if (upstairsActive && downstairsActive) {
    log.trace "updateMode - All On"
    setLocationMode(modeAllOn)
  } else if (!upstairsActive && !downstairsActive) {
    log.trace "updateMode - All Off"
    setLocationMode(modeAllOff)
  } else if (upstairsActive && !downstairsActive) {
    log.trace "updateMode - Upstairs Only"
    setLocationMode(modeOnlyUpstairs)
  } else if (!upstairsActive && downstairsActive) {
    log.trace "updateMode - Downstairs Only"
    setLocationMode(modeOnlyDownstairs)
  }
}

def updatePresence() {
  def person1Active = (person1upstairs.currentValue('switch') == 'on'
      || person1downstairs.currentValue('switch') == 'on')
  def person2Active = (person2upstairs.currentValue('switch') == 'on'
      || person2downstairs.currentValue('switch') == 'on')

  if (person1Active) {
    log.trace "updatePresence - Person 1 - Arrived"
    person1presence.arrived()
  } else {
    log.trace "updatePresence - Person 1 - Departed"
    person1presence.departed()
  }
  if (person2Active) {
    log.trace "updatePresence - Person 2 - Arrived"
    person2presence.arrived()
  } else {
    log.trace "updatePresence - Person 2 - Departed"
    person2presence.departed()
  }
}
