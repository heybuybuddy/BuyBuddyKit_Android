/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2015 Joe Rogers
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
package co.buybuddy.sdk.ble.blecompat;

import java.util.List;

/**
 * Bluetooth LE scan callbacks. Scan results are reported using these callbacks.
 *
 * @see BluetoothLeScannerCompat#startScan
 */

public abstract class ScanCallbackCompat {
    /**
     * Fails to start scan as BLE scan with the same settings is already started by the app.
     */
    public static final int SCAN_FAILED_ALREADY_STARTED = 11;

    /**
     * Fails to start scan as app cannot be registered.
     */
    public static final int SCAN_FAILED_APPLICATION_REGISTRATION_FAILED = 12;

    /**
     * Fails to start scan due an internal error
     */
    public static final int SCAN_FAILED_INTERNAL_ERROR = 13;

    /**
     * Fails to start power optimized scan as this feature is not supported.
     */
    public static final int SCAN_FAILED_FEATURE_UNSUPPORTED = 14;

    /**
     * Callback when a BLE advertisement has been found.
     *
     * @param callbackType Determines how this callback was triggered. Currently could only be
     *                     {@link android.bluetooth.le.ScanSettings#CALLBACK_TYPE_ALL_MATCHES}.
     * @param result       A Bluetooth LE scan result.
     */
    @SuppressWarnings("EmptyMethod")
    public void onScanResult(int callbackType, ScanResultCompat result) {
        // no implementation
    }

    /**
     * Callback when batch results are delivered.
     *
     * @param results List of scan results that are previously scanned.
     */
    @SuppressWarnings("EmptyMethod")
    public void onBatchScanResults(List<ScanResultCompat> results) {
    }

    /**
     * Callback when scan could not be started.
     *
     * @param errorCode Error code (one of SCAN_FAILED_*) for scan failure.
     */
    @SuppressWarnings("EmptyMethod")
    public void onScanFailed(int errorCode) {
    }
}
