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
metadata {
	definition (name: "Ping Sensor", namespace: "natecj", author: "Nathan Jacobson") {
		capability "Presence Sensor"
		capability "Sensor"
		capability "Polling"
	}

	simulator {
		status "present": "presence: 1"
		status "not present": "presence: 0"
	}

	tiles {
		standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
			state("present", labelIcon:"st.presence.tile.mobile-present", backgroundColor:"#53a7c0")
			state("not present", labelIcon:"st.presence.tile.mobile-not-present", backgroundColor:"#ffffff")
		}
		main "presence"
		details "presence"
	}

	preferences {
		section("Device Settings:") {
			input "host", "string", title:"Device to Ping", description: "Host or IP Address", required: true, displayDuringSetup: true
		}
	}
}

def installed() {
  log.info('installed()')
  state.presence = false
}

def updated() {
  log.info('updated()')
  state.presence = false
}

def poll() {
  log.info('poll()')
  if (!settings.host) settings.host = '192.168.11.100'
  log.debug "Pinging host ${settings.host}..."

	def proc = "ping -c 3 ${host}".execute()
	proc.waitFor()
	newState = (proc.exitValue() == 0)

	if (state.presence != newState) {
		state.presence = newState
		if (newState) {
			log.debug "Host was found (false => true)"
			parse("presence: 1")
		} else {
			log.debug "Host was lost (true => false)"
			parse("presence: 0")
		}
	}
}

def parse(String description) {
  log.info('parse()')
	def value = parseValue(description)
	def linkText = getLinkText(device)
	def descriptionText = parseDescriptionText(linkText, value, description)
	def handlerName = getState(value)
	def isStateChange = isStateChange(device, name, value)

	def results = [
		name: "presence",
		value: value,
		unit: null,
		linkText: linkText,
		descriptionText: descriptionText,
		handlerName: handlerName,
		isStateChange: isStateChange,
		displayed: displayed(description, isStateChange)
	]
	log.debug "Parse returned $results.descriptionText"
	return results
}


private String parseValue(String description) {
	switch(description) {
		case "presence: 1": return "present"
		case "presence: 0": return "not present"
		default: return description
	}
}

private parseDescriptionText(String linkText, String value, String description) {
	switch(value) {
		case "present": return "$linkText has arrived"
		case "not present": return "$linkText has left"
		default: return value
	}
}

private getState(String value) {
	switch(value) {
		case "present": return "arrived"
		case "not present": return "left"
		default: return value
	}
}
