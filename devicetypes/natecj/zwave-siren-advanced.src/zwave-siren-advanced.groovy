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
	definition (name: "Z-Wave Siren Advanced", namespace: "natecj", author: "Nathan Jacobson") {
		capability "Actuator"
		capability "Alarm"
		capability "Battery"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
		capability "Switch"

		fingerprint inClusters: "0x20,0x25,0x86,0x80,0x85,0x72,0x71"
	}

	simulator {
		// reply messages
		reply "2001FF,2002": "command: 2003, payload: FF"
		reply "200100,2002": "command: 2003, payload: 00"
		reply "200121,2002": "command: 2003, payload: 21"
		reply "200142,2002": "command: 2003, payload: 42"
		reply "2001FF,delay 3000,200100,2002": "command: 2003, payload: 00"
	}
    
	tiles(scale: 2) {
		multiAttributeTile(name:"alarm", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.alarm", key: "PRIMARY_CONTROL") {
				attributeState "both", label:'alarm!', action:'alarm.off', icon:"st.alarm.alarm.alarm", backgroundColor:"#e86d13"
				attributeState "off", label:'off', action:'alarm.both', icon:"st.alarm.alarm.alarm", backgroundColor:"#ffffff"
			}
            tileAttribute ("statusText", key: "SECONDARY_CONTROL") {
           		attributeState "statusText", label:'${currentValue}'       		
            }
		}

		standardTile("off", "device.alarm", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"alarm.off", icon:"st.secondary.off"
		}
        valueTile("battery", "device.battery", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "battery", label:'${currentValue}% battery', unit:""
		}
        standardTile("refresh", "device.refresh", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
        
		main "alarm"
		details(["alarm", "off", "battery", "refresh"])
	}
}

def createEvents(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "$device.displayName has a low battery"
	} else {
		map.value = cmd.batteryLevel
	}
	state.lastbatt = new Date().time
	createEvent(map)
}

def poll() {
	if (secondsPast(state.lastbatt, 36*60*60)) {
		return zwave.batteryV1.batteryGet().format
	} else {
		return null
	}
}

private Boolean secondsPast(timestamp, seconds) {
	if (!(timestamp instanceof Number)) {
		if (timestamp instanceof Date) {
			timestamp = timestamp.time
		} else if ((timestamp instanceof String) && timestamp.isNumber()) {
			timestamp = timestamp.toLong()
		} else {
			return true
		}
	}
	return (new Date().time - timestamp) > (seconds * 1000)
}

def on() {
  log.debug "sending on"
  [
    zwave.basicV1.basicSet(value: 0xFF).format(),
    zwave.basicV1.basicGet().format()
  ]
}

def off() {
	log.debug "sending off"
	[
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.basicV1.basicGet().format()
	]
}

def strobe() {
	log.debug "sending stobe"
    on()
}

def siren() {
	log.debug "sending siren"
    on()
}

def both() {
	log.debug "sending both"
    on()
}

def refresh() {
	log.debug "sending battery refresh command"
	zwave.batteryV1.batteryGet().format()
}

def parse(String description) {
	log.debug "parse($description)"
	def result = null
	def cmd = zwave.parse(description, [0x20: 1])
	if (cmd) {
		result = createEvents(cmd)
	}
	log.debug "Parse returned ${result?.descriptionText}"
	return result
}

def createEvents(physicalgraph.zwave.commands.basicv1.BasicReport cmd)
{
	def switchValue = cmd.value ? "on" : "off"
	def alarmValue
	if (cmd.value == 0) {
		alarmValue = "off"
	} else if (cmd.value <= 33) {
		alarmValue = "strobe"
	} else if (cmd.value <= 66) {
		alarmValue = "siren"
	} else {
		alarmValue = "both"
	}
	[
		createEvent([name: "switch", value: switchValue, type: "digital", displayed: false]),
		createEvent([name: "alarm", value: alarmValue, type: "digital"])
	]
}


def createEvents(physicalgraph.zwave.Command cmd) {
	log.warn "UNEXPECTED COMMAND: $cmd"
}
