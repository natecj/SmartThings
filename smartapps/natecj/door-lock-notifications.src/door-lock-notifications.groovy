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
  name: "Door Lock Notifications",
  namespace: "natecj",
  author: "Nathan Jacobson",
  description: "Get notifications for a variety of door lock events",
  category: "Safety & Security",
  iconUrl: "http://www.gharexpert.com/mid/4142010105208.jpg",
  iconX2Url: "http://www.gharexpert.com/mid/4142010105208.jpg"
)

import groovy.json.JsonSlurper

preferences {
	section("Choose Locks") {
		input "locks", "capability.lock", multiple: true
	}
  section("SmartApp Settings") {
    icon title: "Choose an Icon", required: false
  }
}

def installed() {
  initialize()
}

def updated() {
  unsubscribe()
  unschedule()
  initialize()
}

def initialize() {
  subscribe(locks, "lock", checkCode)
}

def checkCode(evt) {
  log.debug "Device: $evt.displayName ($evt.deviceId)"
  log.debug "Data: $evt.data"
  log.debug "Value: $evt.value"
  log.debug "State Change: ${evt.isStateChange()}"
  if (evt.data) {
    def lockData = new JsonSlurper().parseText(evt.data)
    log.debug "lockData: ${lockData}"
  }
}
