# Proguard Retrace Unscrambler [![Build & Test & Verify](https://github.com/chrimaeon/proguard-retrace-unscrambler/actions/workflows/main.yml/badge.svg)](https://github.com/chrimaeon/proguard-retrace-unscrambler/actions/workflows/main.yml)

[![License](https://img.shields.io/badge/license-Apache%202.0-brightgreen.svg?style=for-the-badge)](http://www.apache.org/licenses/LICENSE-2.0)
[![JetBrains IntelliJ Plugins](https://img.shields.io/jetbrains/plugin/v/15267-proguard-retrace-unscrambler?style=for-the-badge)][3]
[![JetBrains Plugin Rating](https://img.shields.io/jetbrains/plugin/r/stars/com.cmgapps.intellij.proguard-retrace-unscambler?style=for-the-badge&label=Rating)][3]

This is an [IntelliJ IDEA][1] and [Android Studio][2] Plugin to de-obfuscate your stacktraces.

## Installation

* In the Settings/Preferences dialog, select Plugins.
* Search for `Proguard Retrace Unscrambler`

OR

Download it from the Plugin Marketplace: [Proguard Retrace Unscrambler][3]

## Usage

In IntelliJ IDEA or Android Studio

* Go to _Analyze_
* Select _Analyze Stacktrace…_
* Check _Unscramble stacktrace_
* Select _Proguard Retrace_
* Choose Proguard/R8 mapping file in _Log file_
* Paste stacktrace and Press _OK_

![Screenshot](art/screenshot.png)

## License

```text
Copyright (c) 2020. Christian Grach <christian.grach@cmgapps.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[1]: https://www.jetbrains.com/idea/
[2]: https://developer.android.com/studio/index.html
[3]: https://plugins.jetbrains.com/plugin/15267-proguard-retrace-unscrambler
