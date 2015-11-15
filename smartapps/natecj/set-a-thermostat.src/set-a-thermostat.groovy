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
  name: "Set a Thermostat",
  namespace: "natecj",
  author: "Nathan Jacobson",
  description: "Set a Thermostat.",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	section("Choose thermostat... ") {
		input "thermostat", "capability.thermostat"
	}
	section("Heating setting...") {
		input "heatingSetpoint", "number", title: "Degrees?"
	}
	section("Cooling setting..."){
		input "coolingSetpoint", "number", title: "Degrees?"
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
	subscribe(location, changedLocationMode)
	subscribe(app, appTouch)
}

def changedLocationMode(evt) {
	setThermostat()
}

def appTouch(evt) {
	setThermostat()
}

def setThermostat() {
	thermostat.setHeatingSetpoint(heatingSetpoint)
	thermostat.setCoolingSetpoint(coolingSetpoint)
	thermostat.poll()
}
