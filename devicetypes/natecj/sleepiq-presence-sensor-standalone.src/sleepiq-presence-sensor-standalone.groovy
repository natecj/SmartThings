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
  definition (name: "SleepIQ Presence Sensor Standalone", namespace: "natecj", author: "Nathan Jacobson") {
    capability "Presence Sensor"
    capability "Switch"
    capability "Polling"

    command "arrived"
    command "departed"
  }

  simulator {
    status "present": "presence: present"
    status "not present": "presence: not present"
	status "on": "switch: on"
    status "off": "switch: not off"
  }

  preferences {
    section("Account Settings:") {
      input("login", "text", title: "Username", description: "Your SleepIQ username")
      input("password", "password", title: "Password", description: "Your SleepIQ password")
    }
    section("Bed Settings:") {
      input("bed_index", title: "Bed", "number", required: true, defaultValue: "1", description: "Which bed number is this? (first bed is 1)")
      input("bed_side", title: "Side", "enum", required: true, defaultValue: "Either", options: ["Left", "Right", "Both", "Either"], description: "The side(s) of the bed to monitor")
    }
  }

  tiles {
    standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
      state("not present", label:'not present', icon:"st.presence.tile.not-present", backgroundColor:"#ffffff", action:"arrived")
	  state("present", label:'present', icon:"st.presence.tile.present", backgroundColor:"#53a7c0", action:"departed")
    }
    standardTile("refresh", "device.poll", inactiveLabel: false, decoration: "flat") {
      state "default", action:"polling.poll", icon:"st.secondary.refresh"
    }
    main "presence"
    details(["presence", "refresh"])
  }
}

def installed() {
  log.trace 'installed()'
}

def updated() {
  log.trace 'updated()'
}

def parse(String description) {
  def results = []
  def pair = description.split(":")
  results = createEvent(name: pair[0].trim(), value: pair[1].trim())
  //results = createEvent(name: "contact", value: "closed", descriptionText: "$device.displayName is closed")
  log.debug "\"$description\" parsed to ${results.inspect()}"
  results
}

def poll() {
  log.trace "poll()"
  if (state.session_key && state.cookies) {
    doBedFamilyStatus()
  } else {
    doLogin()
  }
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



def doLogin() {
  log.trace "doLogin()"
  
  def params = [
    uri: 'https://api.sleepiq.sleepnumber.com/rest/login',
    headers: [
      'Content-Type': 'application/json;charset=UTF-8',
      'Host': 'api.sleepiq.sleepnumber.com',
      'DNT': '1',
      'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36',
    ],
    body: '{"login":"' + settings.login + '","password":"' + settings.password + '"}='
  ]
  state.session_key = ''
  state.cookies = ''
  httpPut(params) { response -> doLoginComplete(response) }
}

def doLoginComplete(response) {
  if (response.status != 200) {
    log.error "doLoginComplete(): Request was unsuccessful: ($response.status) $response.data"
    return
  }
  log.debug "doLoginComplete(): Request was successful: ($response.status) $response.data"

  state.session_key = response.data.key
  state.cookies = ''
  response.getHeaders('Set-Cookie').each {
    String cookie = it.value.split(';')[0]
    state.cookies = state.cookies + cookie + ';'
  }
  doBedFamilyStatus()
}

def doBedFamilyStatus() {
  log.trace "doBedFamilyStatus() - $state.cookies"
  
  def params = [
    uri: 'https://api.sleepiq.sleepnumber.com/rest/bed/familyStatus?_k=' + state.session_key,
    headers: [
      'Content-Type': 'application/json;charset=UTF-8',
      'Host': 'api.sleepiq.sleepnumber.com',
      'DNT': '1',
      'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36',
      'Cookie': state.cookies,
    ],
  ]
  try {
    httpGet(params) { response -> 
      state.request_retry_count = null
      doBedFamilyStatusComplete(response)
    }
  } catch(Exception e) {
    if (!state.request_retry_count) state.request_retry_count = 0
	state.request_retry_count += 1
    if (state.request_retry_count < 2) {
      log.debug "Bed Family Status Request failed, retrying"
      doLogin()
    } else {
      log.error "Bed Family Status Request failed"
    }
  }
}

def doBedFamilyStatusComplete(response) {
  if (response.status != 200) {
    log.error "doBedFamilyStatusComplete() Request was unsuccessful: ($response.status) $response.data"
    return
  }
  log.debug "doBedFamilyStatusComplete() Request was successful: ($response.status) $response.data"

  def bed_status = response.data.beds[(settings.bed_index - 1)]
  def bed_presence = false
  switch (settings.bed_side) {
  	case "Left":
      bed_presence = bed_status.leftSide.isInBed
      break
  	case "Right":
      bed_presence = bed_status.rightSide.isInBed
      break
	case "Both":
	  bed_presence = bed_status.leftSide.isInBed && bed_status.rightSide.isInBed
      break
	case "Either":
	  bed_presence = bed_status.leftSide.isInBed || bed_status.rightSide.isInBed
      break
    default:
      log.error("Invalid Bed Side value '$bed_side'")
      return
  }

  if (bed_presence) {
    arrived()
  } else {
    departed()
  }
}
