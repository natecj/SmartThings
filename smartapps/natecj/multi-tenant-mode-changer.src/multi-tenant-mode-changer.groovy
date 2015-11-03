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
  name: "Multi Tenant Mode Changer",
  namespace: "natecj",
  author: "Nathan Jacobson",
  description: "Change modes based on virtual switches representing people in zones.",
  category: "Convenience",
  iconUrl: "",
  iconX2Url: ""
)

preferences {
	section("Switches"){
    input "zone1switches", "capability.switch", title: "Zone Upstairs", multiple: true
    input "zone2switches", "capability.switch", title: "Zone Downstairs", multiple: true
  }
	section("Modes") {
	  input "modeAllOn", "mode", title: "All On", defaultValue: "Home"
	  input "modeAllOff", "mode", title: "All Off", defaultValue: "Away"
	  input "modeOnlyZone1", "mode", title: "Only Upstairs", defaultValue: "Night"
	  input "modeOnlyZone2", "mode", title: "Only Downstairs", defaultValue: "Day"
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
	subscribe(zone1switches, "switch", switchHandler)
	subscribe(zone2switches, "switch", switchHandler)
}

def switchHandler(evt) {
  def zone1on = zone1switches.any{ it.currentValue('switch') == 'on' }
  def zone2on = zone2switches.any{ it.currentValue('switch') == 'on' }

  if (zone1on && zone2on) {
    log.debug("trigger modeAllOn ($modeAllOn)")
    setLocationMode(modeAllOn)
  } else if (!zone1on && !zone2on) {
    log.debug("trigger modeAllOff ($modeAllOff)")
    setLocationMode(modeAllOff)
  } else if (zone1on && !zone2on) {
    log.debug("trigger modeOnlyZone1 ($modeOnlyZone1)")
    setLocationMode(modeOnlyZone1)
  } else if (!zone1on && zone2on) {
    log.debug("trigger modeOnlyZone2 ($modeOnlyZone2)")
    setLocationMode(modeOnlyZone2)
  }
}
