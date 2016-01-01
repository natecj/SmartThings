/**
 *  Copyright 2014 SmartThings
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
	// Automatically generated. Make future change here.
	definition (name: "Simulated Presence Sensor Advanced", namespace: "smartthings", author: "SmartThings") {
		capability "Presence Sensor"
		capability "Switch"

		command "arrived"
		command "departed"
	}

	simulator {
		status "present": "presence: present"
		status "not present": "presence: not present"
		status "on": "switch: on"
		status "off": "switch: not off"
	}

	tiles {
		standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
			state("not present", label:'not present', icon:"st.presence.tile.not-present", backgroundColor:"#ffffff", action:"arrived")
			state("present", label:'present', icon:"st.presence.tile.present", backgroundColor:"#53a7c0", action:"departed")
		}
		main "presence"
		details(["presence"])
	}
}

def parse(String description) {
	def pair = description.split(":")
	createEvent(name: pair[0].trim(), value: pair[1].trim())
}

// handle commands
def arrived() {
  log.trace "arrived()"
  sendEvent(name: "presence", value: "present")
  sendEvent(name: "switch", value: "on")
}

def departed() {
  log.trace "departed()"
  sendEvent(name: "presence", value: "not present")
  sendEvent(name: "switch", value: "off")
}

def on() {
  log.trace "on()"
  arrived()
}

def off() {
  log.trace "off()"
  departed()
}
