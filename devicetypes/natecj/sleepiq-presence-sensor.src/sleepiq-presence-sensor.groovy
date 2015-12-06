/**
 *  SleepIQ Presence Sensor
 *
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
	definition (name: "SleepIQ Presence Sensor", namespace: "natecj", author: "Nathan Jacobson") {
		capability "Presence Sensor"
		capability "Sensor"
		capability "Polling"
	}

  simulator {

  }

  preferences {
    section("SleepIQ Settings:") {
      input("login", "text", title: "Username", description: "Your SleepIQ username (usually an email address)")
      input("password", "password", title: "Password", description: "Your SleepIQ password")
    }
    section("Bed Settings:") {
      input("bed_index", title: "Bed", "number", required: true, defaultValue: "1", description: "Which bed number is this?")
      input("bed_side", title: "Side", "enum", multiple: false, required: true, defaultValue: "Both", options: ["Left", "Right", "Both", "Either"], description: "The side(s) of the bed to monitor")
    }
  }

  tiles {
		standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
			state("present", labelIcon:"st.presence.tile.mobile-present", backgroundColor:"#53a7c0")
			state("not present", labelIcon:"st.presence.tile.mobile-not-present", backgroundColor:"#ffffff")
		}
    standardTile("refresh", "device.poll", inactiveLabel: false, decoration: "flat") {
        state "default", action:"polling.poll", icon:"st.secondary.refresh"
    }

    main "presence"
    details(["presence", "refresh"])
  }
}

def installed() {
  log.info('installed()')
  state.presence = false
  state.session_key = ''
  state.cookies = ''
}

def updated() {
  log.info('updated()')
  state.presence = false
  state.session_key = ''
  state.cookies = ''
}

// parse events into attributes
def parse(String description) {
  log.info('parse() - $description')
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

// handle device commands
def poll() {
  doLogin()
}

def doLogin() {
  def params = [
    uri: 'https://api.sleepiq.sleepnumber.com/rest/login',
    headers: [
      'Content-Type': 'application/json;charset=UTF-8',
      'Host': 'api.sleepiq.sleepnumber.com',
      'DNT': '1',
      'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36',
    ],
    body: '{"login":"' + login + '","password":"' + password + '"}='
  ]
  log.debug "Making request: ${method}: ${params}"
  httpPut(params) { response -> doLoginComplete(response) }
}

def doLoginComplete(response) {
  log.debug "Login Request was successful, $response.status"
  log.debug "Login Response Headers: $response.headers"
  log.debug "Login Response Data: $response.data"
  state.session_key = $response.data.key
  state.cookies = ''
  response.getHeaders('Set-Cookie').each {
    String cookie = it.value.split(';')[0]
    log.debug "Adding cookie to collection: $cookie"
    state.cookies = state.cookies + cookie + ';'
  }
  log.debug "Login Cookies: $state.cookies"
  doBedFamilyStatus()
}

def doBedFamilyStatus() {
  def params = [
    uri: 'https://api.sleepiq.sleepnumber.com/rest/bed/familyStatus',
    headers: [
      'Content-Type': 'application/json;charset=UTF-8',
      'Host': 'api.sleepiq.sleepnumber.com',
      'DNT': '1',
      'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36',
      'Cookie': state.cookies,
    ],
    body: ['_k': state.session_key]
  ]
  log.debug "Making request: ${method}: ${params}"
  httpGet(params) { response -> doBedFamilyStatusComplete(response) }
}

def doBedFamilyStatusComplete(response) {
  log.debug "Status Request was successful, $response.status"
  log.debug "Status Response Headers: $response.headers"
  log.debug "Status Response Data: $response.data"

  bed_status = $response.data.beds[bed_index]
  bed_presence = false
  if (bed_side == "Left") {
    bed_presence = bed_status.leftSide.isInBed
  } else if (bed_side == "Right") {
    bed_presence = bed_status.leftSide.isInBed
  } else if (bed_side == "Both") {
    bed_presence = bed_status.leftSide.isInBed && bed_status.leftSide.isInBed
  } else if (bed_side == "Either") {
    bed_presence = bed_status.leftSide.isInBed || bed_status.leftSide.isInBed
  } else {
    log.error("Invalid Bed Side value '$bed_side'")
  }

  if (bed_presence) {
    log.debug "Bed Presence: $bed_side is yes/true/present"
    parse("presence: 1")
  } else {
    log.debug "Bed Presence: $bed_side is no/false/not-present"
    parse("presence: 0")
  }
}
