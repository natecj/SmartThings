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
  name: "SleepIQ Manager",
  namespace: "natecj",
  author: "Nathan Jacobson",
  description: "Manage sleepers across multiple beds through your SleepIQ account.",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
  page name: "rootPage"
  page name: "configurePage"
}

def rootPage() {
  log.trace "rootPage()"
  
  def devices = state.devices
  def hrefState = devices ? "complete" : ""
  def hrefDescription = ""
  devices.each { deviceId, deviceName ->
    hrefDescription += "${deviceName}\n"
  }
  
  dynamicPage(name: "rootPage", install: devices ? true : false, uninstall: true) {
    section {
      input("login", "text", title: "Username", description: "Your SleepIQ username", defaultValue: "natecj@gmail.com")
      input("password", "password", title: "Password", description: "Your SleepIQ password", defaultValue: "QqiQU9LzCr")
      href("configurePage", title: "Configure SleepIQ Devices", description: hrefDescription, state: hrefState)
      href(url: "https://sleepiq.sleepnumber.com/", title: "Learn More About SleepIQ", description: "https://sleepiq.sleepnumber.com/", style: "external")
    }
    section {
      label title: "Assign a name", required: false
      mode title: "Set for specific mode(s)", required: false
    }
  }
}

def configurePage() {
  log.trace "configurePage()"

  def responseData = getBedData()
  log.debug "Beds: $responseData"

  dynamicPage(name: "configurePage") {
    if (responseData.beds.size() > 0) {
      (1..responseData.beds.size()).each { index ->
        def bed = responseData.beds[index]
        settings.beds = settings.beds ?: [:]
        section("Bed #$index ($bed)") {
          input name: "beds[$index].both", type: "bool", title: "Both", defaultValue: settings.beds[index]?.both
          input name: "beds[$index].either", type: "bool", title: "Either", defaultValue: settings.beds[index]?.either
          input name: "beds[$index].left", type: "bool", title: "Left", defaultValue: settings.beds[index]?.left
          input name: "beds[$index].right", type: "bool", title: "Right", defaultValue: settings.beds[index]?.right
        }
      }
    } else {
      section {
        paragraph "No Beds Found"
      }
    }
  }
}




def installed() {
  log.trace "installed()"
  initialize()
}

def updated() {
  log.trace "updated()"
  unsubscribe()
  unschedule()
  initialize()
}

def initialize() {
  log.trace "initialize()"
  // update data
  // schedule polling
  // schedule("* /15 * * * ?", "getDataFromWattvision") // every 15 minute
}










def getBedData() {
  state.requestData = null
  doStatus()
  while(state.requestData == null) { sleep(1000) }
  def requestData = state.requestData
  state.requestData = null
  requestData
}

def ApiHost() { "api.sleepiq.sleepnumber.com" }

def ApiUriBase() { "https://api.sleepiq.sleepnumber.com" }

def ApiUserAgent() { "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36" }

def ApiStatusParams() {
  [
    uri: ApiUriBase() + '/rest/bed/familyStatus?_k=' + state.session?.key,
    headers: [
      'Content-Type': 'application/json;charset=UTF-8',
      'Host': ApiHost(),
      'User-Agent': ApiUserAgent(),
      'Cookie': state.session?.cookies,
      'DNT': '1',
    ],
  ]
}

def doStatus() {
  log.trace "doStatus()"

  // Login if there isnt an active session
  if (!state.session) {
    doLogin()
    return
  }

  // Make the request
  try {
    httpGet(ApiStatusParams()) { response -> 
      if (response.status == 200) {
        log.trace "doStatus() Success -  Request was successful: ($response.status) $response.data"
        state.requestData = response.data
      } else {
        log.trace "doStatus() Failure - Request was unsuccessful: ($response.status) $response.data"
        state.session = null
        state.requestData = []
      }
    }
  } catch(Exception e) {
    log.error "doStatus() Error ($e)"
    state.session = null
    state.requestData = []
  }
}

def ApiLoginParams() {
  [
    uri: ApiUriBase() + '/rest/login',
    headers: [
      'Content-Type': 'application/json;charset=UTF-8',
      'Host': ApiHost(),
      'User-Agent': ApiUserAgent(),
      'DNT': '1',
    ],
    body: '{"login":"' + settings.login + '","password":"' + settings.password + '"}='
  ]
}

def doLogin() {
  log.trace "doLogin()"
  state.session = null
  try {
    httpPut(ApiLoginParams()) { response ->
      if (response.status == 200) {
        log.trace "doLogin() Success - Request was successful: ($response.status) $response.data"
        state.session = [:]
        state.session.key = response.data.key
        state.session.cookies = ''
        response.getHeaders('Set-Cookie').each {
          state.session.cookies = state.session.cookies + it.value.split(';')[0] + ';'
        }
        doStatus()
      } else {
        log.trace "doLogin() Failure - Request was unsuccessful: ($response.status) $response.data"
        state.session = null
        state.requestData = []
      }
    }
  } catch(Exception e) {
    log.error "doLogin() Error ($e)"
    state.session = null
    state.requestData = []
  }
}

