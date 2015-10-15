/**
 *  Door Lock Code Distress Message
 *
 *  Copyright 2014 skp19
 *
 */
definition(
    name: "Door Lock Code Distress Message",
    namespace: "skp19",
    author: "skp19",
    description: "Sends a text to someone when a specific code is entered",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

import groovy.json.JsonSlurper

preferences {
	section("Choose Locks") {
		input "locks", "capability.lock", multiple: true
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
  log.debug "$evt.value: $evt, $settings"
  def lockData = new JsonSlurper().parseText(evt.data)
  log.debug "lockData: ${lockData}"
}
