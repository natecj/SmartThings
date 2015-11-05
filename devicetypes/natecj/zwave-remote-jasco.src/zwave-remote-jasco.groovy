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
	definition (name: "Z-Wave Remote Jasco", namespace: "natecj", author: "Nathan Jacobson") {
		capability "Actuator"
		capability "Button"
		capability "Configuration"
		capability "Sensor"

		fingerprint deviceId: "0x01"
	}

	simulator {

	}

	tiles {
		standardTile("state", "device.state", width: 2, height: 2) {
			state "connected", label: "", icon: "st.unknown.zwave.remote-controller", backgroundColor: "#ffffff"
		}
        
		standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}

		main "state"
		details(["state", "configure"])
	}
}

def parse(String description) {
	def results = []
	if (description.startsWith("Err")) {
	    results = createEvent(descriptionText:description, displayed:true)
	} else {
		def cmd = zwave.parse(description, [0x2B: 1, 0x80: 1, 0x84: 1])
		if(cmd) results += zwaveEvent(cmd)
		if(!results) results = [ descriptionText: cmd, displayed: false ]
	}
	log.debug("Parsed '$description' to $results")
	return results
}

def buttonEvent(button, held) {
	button = button as Integer
	log.debug "buttonEvent: $device.displayName button $button was held ($held)"
	if (held) {
		createEvent(name: "button", value: "held", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was held", isStateChange: true)
	} else {
		createEvent(name: "button", value: "pushed", data: [buttonNumber: button], descriptionText: "$device.displayName button $button was pushed", isStateChange: true)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd) {
	log.debug "zwaveEvent() sceneactivationv1.SceneActivationSet"
	Integer button = ((cmd.sceneId + 1) / 2) as Integer
	Boolean held = !(cmd.sceneId % 2)
	buttonEvent(button, held)
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfGet cmd) {
	log.debug "zwaveEvent() sceneactuatorconfv1.SceneActuatorConfGet"
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfReport cmd) {
	log.debug "zwaveEvent() sceneactuatorconfv1.SceneActuatorConfReport"
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv1.ConfigurationReport cmd) {
	log.debug "zwaveEvent() configurationv1.ConfigurationReport"
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd) {
	log.debug "zwaveEvent() hailv1.Hail"
}

def zwaveEvent(physicalgraph.zwave.commands.associationv1.AssociationGroupingsReport cmd) {
	log.debug "zwaveEvent() associationv1.AssociationGroupingsReport (${cmd?.supportedGroupings})"
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	log.debug "zwaveEvent() Unknown"
	[ descriptionText: "$device.displayName: $cmd", linkText:device.displayName, displayed: false ]
}

def configure() {
	def commands = [
//        zwave.configurationV1.configurationSet(parameterNumber: 250, scaledConfigurationValue: 1).format(),
//        zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:zwaveHubNodeId).format(),
        zwave.associationV1.associationGroupingsGet().format(),
        zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:1, sceneId:1).format(),
        zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:2, sceneId:2).format(),
        zwave.sceneControllerConfV1.sceneControllerConfSet(groupId:3, sceneId:3).format(),
        zwave.associationV1.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId).format(),
        zwave.associationV1.associationSet(groupingIdentifier: 2, nodeId: zwaveHubNodeId).format(),
        zwave.associationV1.associationSet(groupingIdentifier: 3, nodeId: zwaveHubNodeId).format()
	]
    commands << 
	log.debug("Sending configuration: $commands")
	return delayBetween(commands, 2300)
}
