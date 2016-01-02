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
  log.debug "Devices? " + (devices ? "true" : "false")

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
  // get status and list beds (both/either/left/right) as choices to create as devices
  // (preselect already existing devices)
  log.debug "Login[$settings.login], Password[$settings.password]"

  dynamicPage(name: "configurePage") {
    if (bedCount > 0) {
      (1..bedCount).each { index->
        section("Bed #$index") {
          paragraph "Bed #$index"
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