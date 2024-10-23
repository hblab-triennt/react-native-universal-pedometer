# react-native-universal-pedometer

React Native pedometer support for iOS version 8.0 and higher and Android. The module is CMPedometer wrapper. More info about CMPedometer can be found in https://developer.apple.com/library/ios/documentation/CoreMotion/Reference/CMPedometer_class/

### Branches
| Type Name       | Interface                       |
|-----------------|---------------------------------|
| `master`        | `「AM3時を歩数リセット時刻とするための時刻ずらし」あり版` |
| `no-time-shift` | `「時刻ずらしなし」版`                    |

### Example

> https://github.com/t2tx/pedometer_example

#### _Note_

- Currently typescript is supported.

### Installation

1. `npm install --save @t2tx/react-native-universal-pedometer`

> or `yarn add @t2tx/react-native-universal-pedometer`

2. `cd ios && pod install && cd ..`

##### iOS Configuration

add **NSMotionUsageDescription** on `ios/<your-project>/info.plist`

```
// info.plist
<dict>
	...
	<key>NSMotionUsageDescription</key>
	<string></string>
</dict>
```

##### Android Configuration

add **android.permission.ACTIVITY_RECOGNITION** on `android/app/src/main/AndroidManifest.xml`

```
// AndroidManifest.xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
	...
	<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION"/>
</manifest>
```

### General Usage

```js
import Pedometer from '@t2tx/react-native-universal-pedometer';
```

or

```js
var Pedometer = require('@t2tx/react-native-universal-pedometer');
```

### Methods

| Method Name                      | Arguments                                                    | Notes                                                        |
| -------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| `isStepCountingAvailable`        | callback: `Callback`                                         |                                                              |
| `isDistanceAvailable`            | callback: `Callback`                                         |                                                              |
| `isFloorCountingAvailable`       | callback: `Callback`                                         |                                                              |
| `isPaceAvailable`                | callback: `Callback`                                         |                                                              |
| `isCadenceAvailable`             | callback: `Callback`                                         |                                                              |
| `startPedometerUpdatesFromDate`  | date: `Date.getTime()`, listener: `Listener`                 | start tracking from current time                             |
| `queryPedometerDataBetweenDates` | startDate: `Date.getTime()`, endDate: `Date.getTime()`, callback: `QueryCallback` | query pedometer data from selected date to other selected date |
| `stopPedometerUpdates`           |                                                              | stop pedometer updates                                       |

### Types

| Type Name                 | Interface                                                    |
| ------------------------- | ------------------------------------------------------------ |
| `PedometerInterface`      | `{ startDate: nubmer; endDate: number; numberOfSteps: number; distance: number; }` |
| `Callback`                | `(error: string or null, avaliable: boolean) => any`         |
| `Listener`                | `(data: PedometerInterface) => any` |
| `QueryCallback`                | `(error: string or null, data: PedometerInterface or null) => any` |

