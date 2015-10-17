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
metadata {
	definition (name: "Virtual Garage Door", namespace: "natecj", author: "Nathan Jacobson") {
		capability "Actuator"
		capability "Door Control"
    capability "Garage Door Control"
		capability "Contact Sensor"
		capability "Refresh"
		capability "Sensor"

    command "finishOpening"
		command "finishClosing"
	}

	tiles {
		standardTile("toggle", "device.door", width: 3, height: 2) {
			state("closed", label:'${name}', action:"door control.open", icon:"st.doors.garage.garage-closed", backgroundColor:"#79b821", nextState:"opening")
			state("open", label:'${name}', action:"door control.close", icon:"st.doors.garage.garage-open", backgroundColor:"#ffa81e", nextState:"closing")
			state("opening", label:'${name}', icon:"st.doors.garage.garage-closed", backgroundColor:"#ffe71e")
			state("closing", label:'${name}', icon:"st.doors.garage.garage-open", backgroundColor:"#ffe71e")
		}

		standardTile("open", "device.door", inactiveLabel: false, decoration: "flat") {
			state "default", label:'open', action:"door control.open", icon:"st.doors.garage.garage-opening"
		}
		standardTile("close", "device.door", inactiveLabel: false, decoration: "flat") {
			state "default", label:'close', action:"door control.close", icon:"st.doors.garage.garage-closing"
		}

		main "toggle"
		details(["toggle", "open", "close"])
	}
}

def parse(String description) {
	log.trace "parse($description)"
}

def open() {
  sendEvent(name: "door", value: "opening")
}

def finishOpening() {
  sendEvent(name: "door", value: "open")
  sendEvent(name: "contact", value: "open")
}

def close() {
  sendEvent(name: "door", value: "closing")
}

def finishClosing() {
  sendEvent(name: "door", value: "closed")
  sendEvent(name: "contact", value: "closed")
}
