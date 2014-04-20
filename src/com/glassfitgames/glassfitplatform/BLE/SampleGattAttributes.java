/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.glassfitgames.glassfitplatform.BLE;

import java.util.HashMap;
import java.util.Locale;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // data from developer.bluetooth.org/gatt
        // Services
        attributes.put("1811", "org.bluetooth.service.alert_notification");
        attributes.put("180F", "org.bluetooth.service.battery_service");
        attributes.put("1810", "org.bluetooth.service.blood_pressure");
        attributes.put("1805", "org.bluetooth.service.current_time");
        attributes.put("1818", "org.bluetooth.service.cycling_power");
        attributes.put("1816", "org.bluetooth.service.cycling_speed_and_cadence");
        attributes.put("180A", "org.bluetooth.service.device_information");
        attributes.put("1800", "org.bluetooth.service.generic_access");
        attributes.put("1801", "org.bluetooth.service.generic_attribute");
        attributes.put("1808", "org.bluetooth.service.glucose");
        attributes.put("1809", "org.bluetooth.service.health_thermometer");
        attributes.put("180D", "org.bluetooth.service.heart_rate");
        attributes.put("1812", "org.bluetooth.service.human_interface_device");
        attributes.put("1802", "org.bluetooth.service.immediate_alert");
        attributes.put("1803", "org.bluetooth.service.link_loss");
        attributes.put("1819", "org.bluetooth.service.location_and_navigation");
        attributes.put("1807", "org.bluetooth.service.next_dst_change");
        attributes.put("180E", "org.bluetooth.service.phone_alert_status");
        attributes.put("1806", "org.bluetooth.service.reference_time_update");
        attributes.put("1814", "org.bluetooth.service.running_speed_and_cadence");
        attributes.put("1813", "org.bluetooth.service.scan_parameters");
        attributes.put("1804", "org.bluetooth.service.tx_power");
        
        // Characteristics
        attributes.put("2A00", "org.bluetooth.characteristic.gap.device_name");
        attributes.put("2A01", "org.bluetooth.characteristic.gap.appearance");
        attributes.put("2A02", "org.bluetooth.characteristic.gap.peripheral_privacy_flag");
        attributes.put("2A03", "org.bluetooth.characteristic.gap.reconnection_address");
        attributes.put("2A04", "org.bluetooth.characteristic.gap.peripheral_preferred_connection_parameters");
        attributes.put("2A05", "org.bluetooth.characteristic.gatt.service_changed");
        attributes.put("2A06", "org.bluetooth.characteristic.alert_level");
        attributes.put("2A07", "org.bluetooth.characteristic.tx_power_level");
        attributes.put("2A08", "org.bluetooth.characteristic.date_time");
        attributes.put("2A09", "org.bluetooth.characteristic.day_of_week");
        attributes.put("2A0A", "org.bluetooth.characteristic.day_date_time");
        attributes.put("2A0C", "org.bluetooth.characteristic.exact_time_256");
        attributes.put("2A0D", "org.bluetooth.characteristic.dst_offset");
        attributes.put("2A0E", "org.bluetooth.characteristic.time_zone");
        attributes.put("2A0F", "org.bluetooth.characteristic.local_time_information");
        attributes.put("2A11", "org.bluetooth.characteristic.time_with_dst");
        attributes.put("2A12", "org.bluetooth.characteristic.time_accuracy");
        attributes.put("2A13", "org.bluetooth.characteristic.time_source");
        attributes.put("2A14", "org.bluetooth.characteristic.reference_time_information");
        attributes.put("2A16", "org.bluetooth.characteristic.time_update_control_point");
        attributes.put("2A17", "org.bluetooth.characteristic.time_update_state");
        attributes.put("2A18", "org.bluetooth.characteristic.glucose_measurement");
        attributes.put("2A19", "org.bluetooth.characteristic.battery_level");
        attributes.put("2A1C", "org.bluetooth.characteristic.temperature_measurement");
        attributes.put("2A1D", "org.bluetooth.characteristic.temperature_type");
        attributes.put("2A1E", "org.bluetooth.characteristic.intermediate_temperature");
        attributes.put("2A21", "org.bluetooth.characteristic.measurement_interval");
        attributes.put("2A22", "org.bluetooth.characteristic.boot_keyboard_input_report");
        attributes.put("2A23", "org.bluetooth.characteristic.system_id");
        attributes.put("2A24", "org.bluetooth.characteristic.model_number_string");
        attributes.put("2A25", "org.bluetooth.characteristic.serial_number_string");
        attributes.put("2A26", "org.bluetooth.characteristic.firmware_revision_string");
        attributes.put("2A27", "org.bluetooth.characteristic.hardware_revision_string");
        attributes.put("2A28", "org.bluetooth.characteristic.software_revision_string");
        attributes.put("2A29", "org.bluetooth.characteristic.manufacturer_name_string");
        attributes.put("2A2A", "org.bluetooth.characteristic.ieee_11073-20601_regulatory_certification_data_list");
        attributes.put("2A2B", "org.bluetooth.characteristic.current_time");
        attributes.put("2A31", "org.bluetooth.characteristic.scan_refresh");
        attributes.put("2A32", "org.bluetooth.characteristic.boot_keyboard_output_report");
        attributes.put("2A33", "org.bluetooth.characteristic.boot_mouse_input_report");
        attributes.put("2A34", "org.bluetooth.characteristic.glucose_measurement_context");
        attributes.put("2A35", "org.bluetooth.characteristic.blood_pressure_measurement");
        attributes.put("2A36", "org.bluetooth.characteristic.intermediate_blood_pressure");
        attributes.put("2A37", "org.bluetooth.characteristic.heart_rate_measurement");
        attributes.put("2A38", "org.bluetooth.characteristic.body_sensor_location");
        attributes.put("2A39", "org.bluetooth.characteristic.heart_rate_control_point");
        attributes.put("2A3F", "org.bluetooth.characteristic.alert_status");
        attributes.put("2A40", "org.bluetooth.characteristic.ringer_control_point");
        attributes.put("2A41", "org.bluetooth.characteristic.ringer_setting");
        attributes.put("2A42", "org.bluetooth.characteristic.alert_category_id_bit_mask");
        attributes.put("2A43", "org.bluetooth.characteristic.alert_category_id");
        attributes.put("2A44", "org.bluetooth.characteristic.alert_notification_control_point");
        attributes.put("2A45", "org.bluetooth.characteristic.unread_alert_status");
        attributes.put("2A46", "org.bluetooth.characteristic.new_alert");
        attributes.put("2A47", "org.bluetooth.characteristic.supported_new_alert_category");
        attributes.put("2A48", "org.bluetooth.characteristic.supported_unread_alert_category");
        attributes.put("2A49", "org.bluetooth.characteristic.blood_pressure_feature");
        attributes.put("2A4A", "org.bluetooth.characteristic.hid_information");
        attributes.put("2A4B", "org.bluetooth.characteristic.report_map");
        attributes.put("2A4C", "org.bluetooth.characteristic.hid_control_point");
        attributes.put("2A4D", "org.bluetooth.characteristic.report");
        attributes.put("2A4E", "org.bluetooth.characteristic.protocol_mode");
        attributes.put("2A4F", "org.bluetooth.characteristic.scan_interval_window");
        attributes.put("2A50", "org.bluetooth.characteristic.pnp_id");
        attributes.put("2A51", "org.bluetooth.characteristic.glucose_feature");
        attributes.put("2A52", "org.bluetooth.characteristic.record_access_control_point");
        attributes.put("2A53", "org.bluetooth.characteristic.rsc_measurement");
        attributes.put("2A54", "org.bluetooth.characteristic.rsc_feature");
        attributes.put("2A55", "org.bluetooth.characteristic.sc_control_point");
        attributes.put("2A5B", "org.bluetooth.characteristic.csc_measurement");
        attributes.put("2A5C", "org.bluetooth.characteristic.csc_feature");
        attributes.put("2A5D", "org.bluetooth.characteristic.sensor_location");
        attributes.put("2A63", "org.blueeooth.cycling_power_measurement");
        attributes.put("2A64", "org.bluetooth.characteristic.cycling_power_vector");
        attributes.put("2A65", "org.bluteooth.characteristic.cycling_power_feature");
        attributes.put("2A66", "bluetooth.characteristic.cycling_power_control_point");
        attributes.put("2A67", "org.bluetooth.location_and_speed");
        attributes.put("2A68", "org.bluetooth.characteristic.navigation");
        attributes.put("2A69", "org.bluetooth.position_quality");
        attributes.put("2A6A", "org.bluetooth.characteristic.ln_feature");
        attributes.put("2A6B", "org.bluetooth.ln_control_point");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid.toUpperCase(Locale.ENGLISH));
        return name == null ? defaultName : name;
    }
    
}
